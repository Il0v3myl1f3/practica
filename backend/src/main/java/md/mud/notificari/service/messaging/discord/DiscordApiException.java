package md.mud.notificari.service.messaging.discord;

import java.time.Duration;

/**
 * Discord a raspuns, dar a refuzat. Pastreaza tot ce foloseste la clasificare:
 * statusul HTTP, {@code code}-ul numeric din corp, mesajul si eventualul
 * {@code retry_after}.
 *
 * Panele de transport (DNS, conexiune refuzata, timeout) nu trec prin aici - ele
 * raman exceptiile aruncate de RestClient.
 */
public class DiscordApiException extends RuntimeException {

    private final int httpStatus;
    private final Integer code;
    private final String message;
    private final Duration retryAfter;

    public DiscordApiException(int httpStatus, Integer code, String message, Duration retryAfter) {
        super("Discord " + httpStatus + (message == null || message.isBlank() ? "" : ": " + message));
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
        this.retryAfter = retryAfter;
    }

    public int httpStatus() {
        return httpStatus;
    }

    /** Codul numeric Discord (de exemplu 50007), nu statusul HTTP. */
    public Integer code() {
        return code;
    }

    /** Textul de la Discord, niciodata null. */
    public String message() {
        return message == null ? "" : message;
    }

    public Duration retryAfter() {
        return retryAfter;
    }

    /** {@code retry_after} vine fractionar (secunde); rotunjim in sus, ca sa nu reincercam prea devreme. */
    public static Duration roundUp(Double retryAfterSeconds) {
        if (retryAfterSeconds == null || retryAfterSeconds <= 0) {
            return null;
        }
        return Duration.ofSeconds((long) Math.ceil(retryAfterSeconds));
    }
}
