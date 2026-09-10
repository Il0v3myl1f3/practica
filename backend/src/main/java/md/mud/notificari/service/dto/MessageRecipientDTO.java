package md.mud.notificari.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import md.mud.notificari.domain.enumeration.Channel;
import md.mud.notificari.domain.enumeration.DeliveryStatus;

/**
 * A DTO for the {@link md.mud.notificari.domain.MessageRecipient} entity.
 */
@Schema(
    description = "O linie per (destinatar, canal) efectiv folosit la trimitere.\nAici se materializeaza si override-ul de canal per persoana din pasul 3."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageRecipientDTO implements Serializable {

    private Long id;

    @NotNull
    private Channel channel;

    @NotNull
    private DeliveryStatus status;

    @Size(max = 120)
    private String providerMessageId;

    @Size(max = 500)
    private String errorMessage;

    private Instant sentAt;

    private Instant deliveredAt;

    @NotNull
    private RecipientDTO recipient;

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

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public RecipientDTO getRecipient() {
        return recipient;
    }

    public void setRecipient(RecipientDTO recipient) {
        this.recipient = recipient;
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
        if (!(o instanceof MessageRecipientDTO)) {
            return false;
        }

        MessageRecipientDTO messageRecipientDTO = (MessageRecipientDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, messageRecipientDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageRecipientDTO{" +
            "id=" + getId() +
            ", channel='" + getChannel() + "'" +
            ", status='" + getStatus() + "'" +
            ", providerMessageId='" + getProviderMessageId() + "'" +
            ", errorMessage='" + getErrorMessage() + "'" +
            ", sentAt='" + getSentAt() + "'" +
            ", deliveredAt='" + getDeliveredAt() + "'" +
            ", recipient=" + getRecipient() +
            ", message=" + getMessage() +
            "}";
    }
}
