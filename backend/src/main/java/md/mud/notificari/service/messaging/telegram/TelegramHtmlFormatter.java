package md.mud.notificari.service.messaging.telegram;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Reduce HTML-ul editorului la ce accepta Telegram si il taie in mesaje de cel
 * mult {@value TelegramClient#MESSAGE_LIMIT} caractere.
 *
 * Telegram accepta doar {@code b i u s a code pre blockquote} si respinge
 * <b>tot</b> mesajul cu 400 daca apare altceva - inclusiv un {@code <div>}.
 * Editorul nostru produce insa {@code <div>}, {@code <ul><li>} si, din cauza lui
 * {@code styleWithCSS}, ingrosarea ca {@code <span style="font-weight:700">}.
 * De aceea traducem si stilurile CSS, nu doar tag-urile.
 *
 * Parcurgem DOM-ul cu jsoup, nu cu expresii regulate: span-urile imbricate ale
 * editorului nu se rezolva cu regex, iar un singur tag scapat strica tot mesajul.
 *
 * <b>Invariant:</b> fiecare linie produsa are tag-urile echilibrate in ea insasi -
 * la trecerea pe linie noua tag-urile deschise se inchid si se redeschid. Asta face
 * taierea in bucati sigura: nicio bucata nu incepe si nu se termina in mijlocul unui tag.
 */
public final class TelegramHtmlFormatter {

    /** Elementele care incep pe linie noua. Telegram nu are niciun tag de bloc. */
    private static final List<String> BLOCKS = List.of(
        "p",
        "div",
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6",
        "ul",
        "ol",
        "li",
        "table",
        "tr",
        "section",
        "article",
        "header",
        "footer",
        "figure",
        "figcaption",
        "blockquote",
        "pre",
        "hr"
    );

    /** Nu au ce sa caute intr-un mesaj: nici tag-ul, nici continutul. */
    private static final List<String> DROPPED = List.of("script", "style", "head", "title", "meta", "link", "img", "svg", "video", "audio");

    /** Marcaj pentru o lista fara numere, ca sa nu preia numerotarea unui ol din jur. */
    private static final int UNORDERED = -1;

    /** Spatiul insecabil scris de editor. Ca numar, nu ca literal: in cod ar fi indistinct de un spatiu. */
    private static final char NBSP = (char) 160;

    private TelegramHtmlFormatter() {}

    /** Mesajele gata de trimis, in ordine. */
    public static List<String> chunks(String sourceHtml) {
        Document document = Jsoup.parseBodyFragment(sourceHtml == null ? "" : sourceHtml);
        document.outputSettings().prettyPrint(false);
        Writer writer = new Writer();
        writer.walkChildren(document.body());
        return pack(writer.finish());
    }

    /**
     * Acelasi mesaj, fara niciun tag - calea de rezerva cand Telegram refuza
     * formatarea. Lucreaza pe rezultatul lui {@link #chunks(String)}, nu pe sursa:
     * acolo tag-urile sunt deja ale noastre, deci e de ajuns sa le scoatem.
     *
     * Nu se reia parcurgerea cu jsoup, fiindca ar citi liniile noi ca spatii si ar
     * strange tot mesajul pe un singur rand.
     */
    public static String stripTags(String telegramHtml) {
        String withoutTags = telegramHtml.replaceAll("<[^>]*>", "");
        return withoutTags.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }

    // ------------------------------------------------------------ parcurgerea

    private static final class Writer {

        private final List<String> lines = new ArrayList<>();
        private final StringBuilder line = new StringBuilder();
        private final Deque<String> open = new ArrayDeque<>();
        private final Deque<Integer> listIndex = new ArrayDeque<>();
        private int preDepth;

        private Writer() {
            // starea e in campurile de mai sus
        }

        private void walkChildren(Element parent) {
            for (Node child : parent.childNodes()) {
                walk(child);
            }
        }

        private void walk(Node node) {
            if (node instanceof TextNode text) {
                text(text.getWholeText());
                return;
            }
            if (!(node instanceof Element element)) {
                return;
            }
            String tag = element.normalName();
            if (DROPPED.contains(tag)) {
                return;
            }
            if ("br".equals(tag)) {
                newline();
                return;
            }
            if ("ol".equals(tag)) {
                listIndex.push(0);
            } else if ("ul".equals(tag)) {
                listIndex.push(UNORDERED);
            }
            boolean block = BLOCKS.contains(tag);
            if (block) {
                newline();
            }
            if ("li".equals(tag)) {
                bullet();
            }
            List<String> wrappers = wrappersFor(element, tag);
            wrappers.forEach(this::openTag);
            if ("pre".equals(tag)) {
                preDepth++;
            }
            walkChildren(element);
            if ("pre".equals(tag)) {
                preDepth--;
            }
            for (int i = 0; i < wrappers.size(); i++) {
                closeTag();
            }
            if (block) {
                newline();
            }
            if ("ol".equals(tag) || "ul".equals(tag)) {
                listIndex.pop();
            }
        }

        /** Ce tag-uri Telegram trebuie sa inveleasca elementul: zero, unul sau mai multe. */
        private List<String> wrappersFor(Element element, String tag) {
            List<String> wrappers = new ArrayList<>(2);
            switch (tag) {
                case "b", "strong" -> wrappers.add("b");
                case "i", "em" -> wrappers.add("i");
                case "u", "ins" -> wrappers.add("u");
                case "s", "strike", "del" -> wrappers.add("s");
                case "code", "pre", "blockquote" -> wrappers.add(tag);
                case "a" -> link(element).ifPresent(href -> wrappers.add("a href=\"" + escape(href) + "\""));
                default -> {
                    // restul se desface: textul ramane, tag-ul dispare
                }
            }
            // Editorul scrie ingrosarea ca stil CSS, nu ca tag (styleWithCSS=true).
            String style = element.attr("style").toLowerCase(Locale.ROOT);
            if (!style.isEmpty()) {
                addOnce(wrappers, "b", bold(style));
                addOnce(wrappers, "i", style.contains("font-style: italic") || style.contains("font-style:italic"));
                addOnce(wrappers, "u", style.contains("underline"));
                addOnce(wrappers, "s", style.contains("line-through"));
            }
            return wrappers;
        }

        private void openTag(String tag) {
            open.push(tag);
            line.append('<').append(tag).append('>');
        }

        private void closeTag() {
            String tag = open.poll();
            if (tag != null) {
                line.append("</").append(name(tag)).append('>');
            }
        }

        private void text(String raw) {
            if (raw.isEmpty()) {
                return;
            }
            if (preDepth > 0) {
                // Intr-un bloc de cod spatiile si liniile sunt continut, nu formatare.
                String[] parts = raw.split("\n", -1);
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        newline();
                    }
                    line.append(escape(parts[i]));
                }
                return;
            }
            String collapsed = raw.replace(NBSP, ' ').replaceAll("\\s+", " ");
            if (line.isEmpty() && collapsed.startsWith(" ")) {
                collapsed = collapsed.substring(1);
            }
            if (collapsed.isEmpty()) {
                return;
            }
            line.append(escape(collapsed));
        }

        private void bullet() {
            Integer index = listIndex.peek();
            if (index == null || index == UNORDERED) {
                line.append("• ");
                return;
            }
            listIndex.pop();
            listIndex.push(index + 1);
            line.append(index + 1).append(". ");
        }

        /**
         * Inchide tag-urile deschise, trece pe linie noua si le redeschide - ca
         * fiecare linie sa fie valida singura.
         */
        private void newline() {
            if (line.isEmpty() && lines.isEmpty()) {
                return;
            }
            List<String> reopen = new ArrayList<>(open);
            while (!open.isEmpty()) {
                closeTag();
            }
            lines.add(line.toString().stripTrailing());
            line.setLength(0);
            // reopen e in ordinea "cel mai interior primul", deci se parcurge invers
            for (int i = reopen.size() - 1; i >= 0; i--) {
                openTag(reopen.get(i));
            }
        }

        private List<String> finish() {
            while (!open.isEmpty()) {
                closeTag();
            }
            lines.add(line.toString().stripTrailing());
            line.setLength(0);
            return normalize(lines);
        }

    }

    // -------------------------------------------------------------- ajutoare

    private static void addOnce(List<String> wrappers, String tag, boolean condition) {
        if (condition && !wrappers.contains(tag)) {
            wrappers.add(tag);
        }
    }

    /** Doua linii goale la rand devin una; la inceput si la final nu ramane nicio linie goala. */
    private static List<String> normalize(List<String> lines) {
        List<String> out = new ArrayList<>(lines.size());
        for (String raw : lines) {
            String current = raw.strip().isEmpty() ? "" : raw;
            boolean previousEmpty = !out.isEmpty() && out.getLast().isEmpty();
            if (current.isEmpty() && (out.isEmpty() || previousEmpty)) {
                continue;
            }
            out.add(current);
        }
        while (!out.isEmpty() && out.getLast().isEmpty()) {
            out.removeLast();
        }
        return out;
    }

    /** Impacheteaza liniile in mesaje de cel mult {@value TelegramClient#MESSAGE_LIMIT} caractere. */
    private static List<String> pack(List<String> lines) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            for (String piece : split(line)) {
                if (current.isEmpty() && piece.isEmpty()) {
                    continue;
                }
                int needed = current.isEmpty() ? piece.length() : current.length() + 1 + piece.length();
                if (needed > TelegramClient.MESSAGE_LIMIT && !current.isEmpty()) {
                    chunks.add(current.toString());
                    current.setLength(0);
                    if (piece.isEmpty()) {
                        continue;
                    }
                }
                if (!current.isEmpty()) {
                    current.append('\n');
                }
                current.append(piece);
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    /**
     * O singura linie mai lunga decat un mesaj: se taie pe spatii, <b>niciodata in
     * interiorul unui tag</b>, iar tag-urile deschise se inchid la capatul bucatii
     * si se redeschid in urmatoarea.
     */
    private static List<String> split(String line) {
        if (line.length() <= TelegramClient.MESSAGE_LIMIT) {
            return List.of(line);
        }
        List<String> pieces = new ArrayList<>();
        String rest = line;
        while (rest.length() > TelegramClient.MESSAGE_LIMIT) {
            Deque<String> openTags = new ArrayDeque<>();
            Deque<String> tagsAtCut = new ArrayDeque<>();
            int cut = -1;
            int lastOutsideTag = -1;
            for (int i = 0; i < rest.length(); i++) {
                char c = rest.charAt(i);
                if (c == '<') {
                    int end = rest.indexOf('>', i);
                    if (end < 0) {
                        break;
                    }
                    String tag = rest.substring(i + 1, end);
                    if (tag.startsWith("/")) {
                        openTags.poll();
                    } else {
                        openTags.push(tag);
                    }
                    i = end;
                    continue;
                }
                if (i + closingLength(openTags) > TelegramClient.MESSAGE_LIMIT) {
                    break;
                }
                if (c == ' ') {
                    cut = i;
                    tagsAtCut = new ArrayDeque<>(openTags);
                }
                lastOutsideTag = i;
            }
            if (cut <= 0) {
                // Un singur cuvant imens: taiem unde am ajuns, tot in afara unui tag.
                cut = Math.max(1, lastOutsideTag);
                tagsAtCut = new ArrayDeque<>(openTags);
            }
            pieces.add(rest.substring(0, cut) + closing(tagsAtCut));
            rest = opening(tagsAtCut) + rest.substring(cut).stripLeading();
        }
        pieces.add(rest);
        return pieces;
    }

    private static int closingLength(Deque<String> openTags) {
        int length = 0;
        for (String tag : openTags) {
            length += name(tag).length() + 3;
        }
        return length;
    }

    private static String closing(Deque<String> openTags) {
        StringBuilder sb = new StringBuilder();
        openTags.forEach(tag -> sb.append("</").append(name(tag)).append('>'));
        return sb.toString();
    }

    private static String opening(Deque<String> openTags) {
        List<String> ordered = new ArrayList<>(openTags);
        StringBuilder sb = new StringBuilder();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            sb.append('<').append(ordered.get(i)).append('>');
        }
        return sb.toString();
    }

    /** {@code a href="..."} -> {@code a}: numele folosit la inchidere. */
    private static String name(String tag) {
        int space = tag.indexOf(' ');
        return space < 0 ? tag : tag.substring(0, space);
    }

    private static boolean bold(String style) {
        if (style.contains("font-weight: bold") || style.contains("font-weight:bold")) {
            return true;
        }
        for (String weight : List.of("600", "700", "800", "900")) {
            if (style.contains("font-weight: " + weight) || style.contains("font-weight:" + weight)) {
                return true;
            }
        }
        return false;
    }

    /** Doar schemele pe care Telegram le accepta intr-un link. */
    private static Optional<String> link(Element element) {
        String href = element.attr("href").trim();
        String lower = href.toLowerCase(Locale.ROOT);
        boolean safe = lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("mailto:") || lower.startsWith("tg://");
        return safe ? Optional.of(href) : Optional.empty();
    }

    /** Pentru text care intra in sursa HTML, de exemplu subiectul pus prima linie. */
    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
