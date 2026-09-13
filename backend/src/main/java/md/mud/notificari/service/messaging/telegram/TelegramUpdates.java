package md.mud.notificari.service.messaging.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Raspunsul la {@code getUpdates} - de aici aflam chat ID-urile oamenilor care au
 * scris botului. Un bot nu poate scrie primul nimanui, deci asta e singura sursa
 * de chat ID-uri.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUpdates(
    boolean ok,
    @JsonProperty("error_code") Integer errorCode,
    String description,
    TelegramResponse.Parameters parameters,
    List<Update> result
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Update(@JsonProperty("update_id") Long updateId, Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(@JsonProperty("message_id") Long messageId, Long date, Chat chat, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chat(
        Long id,
        String type,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String username,
        String title
    ) {
        /** Numele de afisat: persoana are prenume+nume, un grup are titlu. */
        public String displayName() {
            String person = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
            if (!person.isBlank()) {
                return person;
            }
            if (title != null && !title.isBlank()) {
                return title;
            }
            return username == null || username.isBlank() ? String.valueOf(id) : "@" + username;
        }
    }
}
