package md.mud.notificari.service.messaging.discord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raspunsurile Discord API v10 folosite de {@link DiscordClient}. Fiecare apel
 * are alta forma de raspuns - nu un singur invelis cu {@code ok}, ca la
 * Telegram: reusita sau esecul se afla din codul HTTP.
 */
public final class DiscordResponses {

    private DiscordResponses() {}

    /** Raspunsul la deschiderea unui DM. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Channel(String id) {}

    /** Raspunsul la trimiterea unui mesaj. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(String id, String username, @JsonProperty("global_name") String globalName, Boolean bot) {}

    /** Un membru al serverului, pentru ecranul de conectare. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Member(User user, String nick) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Guild(String name) {}

    /** Corpul de eroare: {@code {code, message, retry_after?}}. {@code retry_after} e in secunde, poate fi fractionar. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(Integer code, String message, @JsonProperty("retry_after") Double retryAfter) {}
}
