package md.mud.notificari.service.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import md.mud.notificari.errors.MessageTemplateException;
import md.mud.notificari.service.app.AppDtos.TemplateView;
import org.springframework.stereotype.Service;

/**
 * Sabloanele intra si ies ca arhiva ZIP cu cate un fisier HTML complet fiecare.
 *
 * Fiecare fisier se deschide singur in browser si se poate edita in orice
 * editor: numele sablonului e numele fisierului, subiectul e in title,
 * descrierea intr-un meta, iar corpul mesajului in body.
 *
 * ponytail: citirea HTML-ului se face cu expresii regulate, nu cu un parser.
 * Documentele sunt generate de noi si au o structura fixa; daca ajungem sa
 * acceptam HTML arbitrar exportat din alte unelte, aici intra jsoup.
 */
@Service
public class TemplateArchiveService {

    private static final Pattern TITLE = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern DESCRIPTION = Pattern.compile(
        "<meta\\s+name=[\"']description[\"']\\s+content=[\"'](.*?)[\"']",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    // Numele fisierului nu poate contine / \ : * ? " < > | , iar un sablon numit
    // "Reamintire: raport" e cu totul plauzibil. Numele adevarat merge intr-un
    // meta, ca dus-intorsul prin arhiva sa nu-l stalceasca.
    private static final Pattern NAME = Pattern.compile(
        "<meta\\s+name=[\"']template-name[\"']\\s+content=[\"'](.*?)[\"']",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern BODY = Pattern.compile("<body[^>]*>(.*?)</body>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public byte[] export(List<TemplateView> templates) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Set<String> used = new HashSet<>();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            for (TemplateView t : templates) {
                zip.putNextEntry(new ZipEntry(uniqueFileName(t.name(), used)));
                zip.write(toHtml(t).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Arhiva nu a putut fi creata.", e);
        }
        return out.toByteArray();
    }

    public List<TemplateView> read(InputStream source) {
        List<TemplateView> found = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(source, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = baseName(entry.getName());
                if (entry.isDirectory() || !entry.getName().toLowerCase(Locale.ROOT).endsWith(".html")) {
                    continue;
                }
                String html = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                found.add(fromHtml(name, html));
            }
        } catch (IOException e) {
            throw MessageTemplateException.invalidArchive();
        }
        if (found.isEmpty()) {
            throw MessageTemplateException.emptyArchive();
        }
        return found;
    }

    private String toHtml(TemplateView t) {
        return (
            "<!doctype html>\n<html lang=\"ro\">\n<head>\n" +
            "<meta charset=\"utf-8\">\n" +
            "<title>" +
            escape(blank(t.subject(), t.name())) +
            "</title>\n" +
            "<meta name=\"description\" content=\"" +
            escape(blank(t.description(), "")) +
            "\">\n" +
            "<meta name=\"template-name\" content=\"" +
            escape(t.name()) +
            "\">\n</head>\n<body>\n" +
            blank(t.body(), "") +
            "\n</body>\n</html>\n"
        );
    }

    private TemplateView fromHtml(String fileName, String html) {
        // Numele din meta bate numele fisierului: fisierul nu poate purta toate
        // caracterele, dar cine editeaza arhiva de mana poate sterge meta-ul.
        String name = blank(unescape(first(NAME, html)), fileName);
        String subject = first(TITLE, html);
        String description = first(DESCRIPTION, html);
        String body = first(BODY, html);
        // Un fisier fara <body> e tratat ca fiind numai corpul mesajului, ca sa
        // putem importa si fragmente HTML scrise de mana.
        if (body.isEmpty() && !html.toLowerCase(Locale.ROOT).contains("<body")) {
            body = html.trim();
        }
        return new TemplateView(null, name, unescape(description), unescape(subject), body.trim());
    }

    private String first(Pattern p, String html) {
        Matcher m = p.matcher(html);
        return m.find() ? m.group(1).trim() : "";
    }

    private String uniqueFileName(String name, Set<String> used) {
        String base = name.replaceAll("[\\\\/:*?\"<>|]", "-").trim();
        if (base.isEmpty()) {
            base = "sablon";
        }
        String candidate = base;
        int n = 2;
        while (!used.add(candidate.toLowerCase(Locale.ROOT))) {
            candidate = base + "-" + n++;
        }
        return candidate + ".html";
    }

    private static String baseName(String entryName) {
        String file = entryName.replace('\\', '/');
        file = file.substring(file.lastIndexOf('/') + 1);
        return file.replaceFirst("(?i)\\.html$", "");
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String unescape(String s) {
        return s.replace("&quot;", "\"").replace("&gt;", ">").replace("&lt;", "<").replace("&amp;", "&");
    }
}
