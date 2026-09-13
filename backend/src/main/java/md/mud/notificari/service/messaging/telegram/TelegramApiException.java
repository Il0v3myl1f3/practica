package md.mud.notificari.service.messaging.telegram;

import java.time.Duration;

/**
 * Telegram a raspuns, dar a refuzat. Pastreaza tot ce foloseste la clasificare:
 * statusul HTTP, {@code error_code}, descrierea si eventualul {@code retry_after}.
 *
 * Panele de transport (DNS, conexiune refuzata, timeout) nu trec prin aici - ele
 * raman exceptiile aruncate de RestClient.
 */
public class TelegramApiException extends RuntimeException {

    private final int httpStatus;
    private final Integer errorCode;
    private final String description;
    private final Duration retryAfter;

    public TelegramApiException(int httpStatus, Integer errorCode, String description, Integer retryAfterSeconds) {
        super("Telegram " + httpStatus + (description == null || description.isBlank() ? "" : ": " + description));
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.description = description;
        this.retryAfter = retryAfterSeconds == null || retryAfterSeconds <= 0 ? null : Duration.ofSeconds(retryAfterSeconds);
    }

    public int httpStatus() {
        return httpStatus;
    }

    public Integer errorCode() {
        return errorCode;
    }

    /** Textul de la Telegram ("Bad Request: chat not found"), niciodata null. */
    public String description() {
        return description == null ? "" : description;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
