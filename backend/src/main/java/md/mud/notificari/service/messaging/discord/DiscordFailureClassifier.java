package md.mud.notificari.service.messaging.discord;

import java.io.IOException;
import java.util.List;
import javax.net.ssl.SSLException;
import md.mud.notificari.service.messaging.SendOutcome;
import org.springframework.core.NestedExceptionUtils;

/**
 * Decide daca un refuz de la Discord merita reincercat. Acelasi rol ca
 * {@code TelegramFailureClassifier}, alta taxonomie: Discord da coduri
 * numerice stabile ({@code code}), nu text de cautat cu {@code contains}.
 *
 * Regula de fond: ce nu se schimba la reincercare e definitiv. Un utilizator
 * care a refuzat DM-urile sau un token revocat raman la fel si peste o ora; o
 * limita de rata sau o pana de retea trec.
 */
public final class DiscordFailureClassifier {

    /**
     * Coduri Discord care nu se schimba la reincercare:
     * 50007 - DM-uri refuzate sau niciun server comun cu botul
     * 10013 - Unknown User
     * 10003 - Unknown Channel
     * 50035 - Invalid Form Body
     * 40005 - Request entity too large
     */
    private static final List<Integer> PERMANENT_CODES = List.of(50007, 10013, 10003, 50035, 40005);

    private DiscordFailureClassifier() {}

    public static SendOutcome classify(Exception e) {
        if (e instanceof DiscordApiException api) {
            return classifyApi(api);
        }
        Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
        if (root instanceof IOException || root instanceof SSLException) {
            // Pana de transport: conexiune refuzata, DNS, timeout, TLS.
            return SendOutcome.retry(describe(root));
        }
        return SendOutcome.retry(describe(e));
    }

    private static SendOutcome classifyApi(DiscordApiException api) {
        int status = api.httpStatus();
        if (status == 429) {
            // Discord spune singur cat sa asteptam, in retry_after.
            return SendOutcome.retry("Limita de rata Discord: " + api.message(), api.retryAfter());
        }
        if (status == 401) {
            return SendOutcome.permanent(api.getMessage());
        }
        if (status == 413 || (api.code() != null && PERMANENT_CODES.contains(api.code()))) {
            return SendOutcome.permanent(api.getMessage());
        }
        if (status == 400 || status == 403 || status == 404) {
            return SendOutcome.permanent(api.getMessage());
        }
        if (status >= 500) {
            return SendOutcome.retry(api.getMessage(), api.retryAfter());
        }
        return SendOutcome.retry(api.getMessage(), api.retryAfter());
    }

    private static String describe(Throwable t) {
        String message = t.getMessage();
        return message == null || message.isBlank() ? t.getClass().getSimpleName() : message;
    }
}
