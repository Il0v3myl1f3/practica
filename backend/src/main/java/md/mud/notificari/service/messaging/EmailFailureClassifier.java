package md.mud.notificari.service.messaging;

import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;

/**
 * Decide daca un esec SMTP merita reincercat.
 *
 * Regula de fond: codurile SMTP 5xx sunt definitive (cutie inexistenta, mesaj
 * respins, autentificare refuzata), 4xx sunt trecatoare (greylisting, limita de
 * rata). Cand nu stim, reincercam - {@code max-attempts} limiteaza dauna, iar
 * eroarea ramane scrisa in istoric.
 *
 * Codul se citeste din textul erorii, nu din {@code SMTPSendFailedException}:
 * implementarea Angus e dependinta de <em>runtime</em> in Spring Boot 4, deci
 * clasele ei nu sunt pe classpath-ul de compilare. Textul incepe insa mereu cu
 * codul ("550-5.1.1 ...", "535 5.7.8 ..."), ceea ce e suficient.
 */
public final class EmailFailureClassifier {

    /** Codul SMTP la inceput de linie, urmat de spatiu sau de cratima (raspuns pe mai multe linii). */
    private static final Pattern SMTP_CODE = Pattern.compile("(?m)^\\s*([45]\\d\\d)(?:[ -]|$)");

    private EmailFailureClassifier() {}

    public static SendOutcome classify(Exception e) {
        // Parola de aplicatie gresita sau revocata. Reincercarile doar atrag
        // atentia Google asupra contului, nu rezolva nimic.
        if (e instanceof MailAuthenticationException) {
            return SendOutcome.permanent("Autentificare SMTP esuata: " + e.getMessage());
        }
        if (e instanceof MailParseException || e instanceof MailPreparationException) {
            return SendOutcome.permanent("Mesaj invalid: " + e.getMessage());
        }

        // Serverul a numit explicit adrese invalide: definitiv, indiferent de cod.
        SendFailedException rejected = findRejection(e);
        if (rejected != null && rejected.getInvalidAddresses() != null && rejected.getInvalidAddresses().length > 0) {
            return SendOutcome.permanent("Adresa respinsa: " + rejected.getMessage());
        }

        SendOutcome bySmtpCode = classifyByCode(e);
        if (bySmtpCode != null) {
            return bySmtpCode;
        }

        Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
        if (root instanceof AddressException) {
            return SendOutcome.permanent("Adresa invalida: " + root.getMessage());
        }
        // Pana de transport: conexiune refuzata, DNS, timeout, TLS.
        if (root instanceof IOException || root instanceof SSLException) {
            return SendOutcome.retry(describe(root));
        }
        return SendOutcome.retry(describe(e));
    }

    /** Primul cod SMTP gasit in lantul de exceptii, inclusiv in mesajele per-destinatar. */
    private static SendOutcome classifyByCode(Exception e) {
        if (e instanceof MailSendException mse) {
            for (Exception failure : mse.getFailedMessages().values()) {
                SendOutcome outcome = codeIn(failure);
                if (outcome != null) {
                    return outcome;
                }
            }
        }
        return codeIn(e);
    }

    private static SendOutcome codeIn(Throwable t) {
        for (Throwable current = t; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null) {
                continue;
            }
            Matcher m = SMTP_CODE.matcher(message);
            if (m.find()) {
                String code = m.group(1);
                String text = firstLine(message);
                return code.startsWith("5") ? SendOutcome.permanent(text) : SendOutcome.retry(text);
            }
        }
        return null;
    }

    private static SendFailedException findRejection(Throwable t) {
        for (Throwable current = t; current != null; current = current.getCause()) {
            if (current instanceof SendFailedException sfe) {
                return sfe;
            }
            if (current instanceof MailSendException mse) {
                for (Exception failure : mse.getFailedMessages().values()) {
                    SendFailedException nested = findRejection(failure);
                    if (nested != null) {
                        return nested;
                    }
                }
            }
        }
        return null;
    }

    private static String describe(Throwable t) {
        String message = t.getMessage();
        return message == null || message.isBlank() ? t.getClass().getSimpleName() : message;
    }

    private static String firstLine(String message) {
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline).trim();
    }
}
