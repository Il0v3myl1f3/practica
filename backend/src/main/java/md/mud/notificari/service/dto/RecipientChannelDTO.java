package md.mud.notificari.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import md.mud.notificari.domain.enumeration.Channel;

/**
 * A DTO for the {@link md.mud.notificari.domain.RecipientChannel} entity.
 */
@Schema(
    description = "address = adresa email / telegram chat id / telefon E.164.\nactive = false inseamna canal neconfigurat pentru acest destinatar."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RecipientChannelDTO implements Serializable {

    private Long id;

    @NotNull
    private Channel channel;

    @NotNull
    @Size(max = 254)
    private String address;

    private Boolean verified;

    @NotNull
    private Boolean active;

    @NotNull
    private RecipientDTO recipient;

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public RecipientDTO getRecipient() {
        return recipient;
    }

    public void setRecipient(RecipientDTO recipient) {
        this.recipient = recipient;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecipientChannelDTO)) {
            return false;
        }

        RecipientChannelDTO recipientChannelDTO = (RecipientChannelDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, recipientChannelDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RecipientChannelDTO{" +
            "id=" + getId() +
            ", channel='" + getChannel() + "'" +
            ", address='" + getAddress() + "'" +
            ", verified='" + getVerified() + "'" +
            ", active='" + getActive() + "'" +
            ", recipient=" + getRecipient() +
            "}";
    }
}
