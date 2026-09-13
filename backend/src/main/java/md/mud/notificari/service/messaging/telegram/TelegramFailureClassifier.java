package md.mud.notificari.service.messaging.telegram;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLException;
import md.mud.notificari.service.messaging.SendOutcome;
import org.springframework.core.NestedExceptionUtils;

/**
 * Decide daca un refuz de la Telegram merita reincercat. Acelasi rol ca
 * {@code EmailFailureClassifier}, alta taxonomie.
 *
 * Regula de fond: ce nu se schimba la reincercare e definitiv. Un chat inexistent,
 * un bot blocat sau un token revocat raman la fel si peste o ora; o limita de rata
 * sau o pana de retea trec. Cand nu stim, reincercam - {@code max-attempts}
 * limiteaza dauna, iar descrierea ramane scrisa in istoric.
 */
public final class TelegramFailureClassifier {

    /** Motive pentru care mesajul nu va ajunge niciodata la acest chat. */
    private static final List<String> PERMANENT = List.of(
        "chat not found",
        "chat_id is empty",
        "user not found",
        "bot was blocked by the user",
        "bot can't initiate conversation",
        "user is deactivated",
        "bot was kicked",
        "not enough rights",
        "message text is empty",
        "file is too big",
        "wrong file identifier",
        "unauthorized"
    );

    /** Formatarea a fost respinsa: merita o a doua incercare, in text curat. */
    private static final String PARSE_FAILURE = "can't parse entities";

    private TelegramFailureClassifier() {}

    public static SendOutcome classify(Exception e) {
        if (e instanceof TelegramApiException api) {
            return classifyApi(api);
        }
        Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
        if (root instanceof IOException || root instanceof SSLException) {
            // Pana de transport: conexiune refuzata, DNS, timeout, TLS.
            return SendOutcome.retry(describe(root));
        }
        return SendOutcome.retry(describe(e));
    }

    /** Telegram a respins formatarea; sender-ul incearca o data in text curat. */
    public static boolean formattingRejected(Exception e) {
        return e instanceof TelegramApiException api && api.httpStatus() == 400 && lower(api.description()).contains(PARSE_FAILURE);
    }

    private static SendOutcome classifyApi(TelegramApiException api) {
        String description = lower(api.description());
        int status = api.httpStatus();
        if (status == 429) {
            // Telegram spune singur cat sa asteptam; poate fi mult mai mult decat backoff-ul nostru.
            return SendOutcome.retry("Limita de rata Telegram: " + api.description(), api.retryAfter());
        }
        if (status == 401 || status == 403 || status == 413 || status == 404) {
            return SendOutcome.permanent(api.getMessage());
        }
        if (PERMANENT.stream().anyMatch(description::contains)) {
            return SendOutcome.permanent(api.getMessage());
        }
        if (status == 400) {
            // Inclusiv "can't parse entities", cand nici textul curat n-a trecut.
            return SendOutcome.permanent(api.getMessage());
        }
        return SendOutcome.retry(api.getMessage(), api.retryAfter());
    }

    private static String lower(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private static String describe(Throwable t) {
        String message = t.getMessage();
        return message == null || message.isBlank() ? t.getClass().getSimpleName() : message;
    }
}
