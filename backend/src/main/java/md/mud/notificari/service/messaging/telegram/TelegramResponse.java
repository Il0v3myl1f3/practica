package md.mud.notificari.service.messaging.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Invelisul raspunsurilor Telegram pentru {@code sendMessage}, {@code sendDocument}
 * si {@code getMe}. Acelasi format si la succes si la eroare: la eroare
 * {@code ok} e false si sosesc {@code error_code} + {@code description}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramResponse(
    boolean ok,
    @JsonProperty("error_code") Integer errorCode,
    String description,
    Parameters parameters,
    Result result
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(@JsonProperty("message_id") Long messageId, String username) {}

    /** Doar {@code retry_after} ne interesa: cate secunde cere Telegram sa asteptam. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Parameters(@JsonProperty("retry_after") Integer retryAfter) {}
}
