package md.mud.notificari.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import md.mud.notificari.domain.enumeration.Channel;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * address = adresa email / telegram chat id / telefon E.164.
 * active = false inseamna canal neconfigurat pentru acest destinatar.
 */
@Entity
@Table(name = "recipient_channel")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RecipientChannel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Channel channel;

    @NotNull
    @Size(max = 254)
    @Column(name = "address", length = 254, nullable = false)
    private String address;

    @Column(name = "verified")
    private Boolean verified;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "channelses", "organization", "recipientGroup" }, allowSetters = true)
    private Recipient recipient;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public RecipientChannel id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public RecipientChannel channel(Channel channel) {
        this.setChannel(channel);
        return this;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getAddress() {
        return this.address;
    }

    public RecipientChannel address(String address) {
        this.setAddress(address);
        return this;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getVerified() {
        return this.verified;
    }

    public RecipientChannel verified(Boolean verified) {
        this.setVerified(verified);
        return this;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public Boolean getActive() {
        return this.active;
    }

    public RecipientChannel active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Recipient getRecipient() {
        return this.recipient;
    }

    public void setRecipient(Recipient recipient) {
        this.recipient = recipient;
    }

    public RecipientChannel recipient(Recipient recipient) {
        this.setRecipient(recipient);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecipientChannel)) {
            return false;
        }
        return getId() != null && getId().equals(((RecipientChannel) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RecipientChannel{" +
            "id=" + getId() +
            ", channel='" + getChannel() + "'" +
            ", address='" + getAddress() + "'" +
            ", verified='" + getVerified() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
