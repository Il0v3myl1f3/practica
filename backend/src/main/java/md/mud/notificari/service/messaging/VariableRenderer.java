package md.mud.notificari.service.messaging;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import md.mud.notificari.domain.Recipient;
import org.springframework.stereotype.Service;

/**
 * Replaces the personalisation variables in a message body, per recipient.
 *
 * The editor stores each variable as a chip: a span carrying data-var, whose
 * text is the {{token}} itself. Substituting the raw token therefore covers
 * both chips and tokens someone typed by hand.
 */
@Service
public class VariableRenderer {

    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*(nume|prenume|grup|email)\\s*}}");

    public String render(String template, Recipient recipient) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        Map<String, String> values = Map.of(
            "nume", nullToEmpty(recipient.getLastName()),
            "prenume", nullToEmpty(recipient.getFirstName()),
            "grup", recipient.getRecipientGroup() == null ? "" : nullToEmpty(recipient.getRecipientGroup().getName()),
            "email", nullToEmpty(recipient.getEmail())
        );
        Matcher m = TOKEN.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(values.getOrDefault(m.group(1), "")));
        }
        m.appendTail(out);
        return out.toString();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
