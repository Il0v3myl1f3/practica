package md.mud.notificari.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import md.mud.notificari.domain.enumeration.Channel;

/**
 * A DTO for the {@link md.mud.notificari.domain.MessageChannel} entity.
 */
@Schema(
    description = "Canalele alese pentru mesaj. Tabela separata pentru ca JDL nu are\ncolectii de enum-uri, iar istoricul se filtreaza dupa canal in DB."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageChannelDTO implements Serializable {

    private Long id;

    @NotNull
    private Channel channel;

    @NotNull
    private MessageDTO message;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public MessageDTO getMessage() {
        return message;
    }

    public void setMessage(MessageDTO message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageChannelDTO)) {
            return false;
        }

        MessageChannelDTO messageChannelDTO = (MessageChannelDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, messageChannelDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageChannelDTO{" +
            "id=" + getId() +
            ", channel='" + getChannel() + "'" +
            ", message=" + getMessage() +
            "}";
    }
}
