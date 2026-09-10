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
 * Canalele alese pentru mesaj. Tabela separata pentru ca JDL nu are
 * colectii de enum-uri, iar istoricul se filtreaza dupa canal in DB.
 */
@Entity
@Table(name = "message_channel")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageChannel implements Serializable {

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

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(
        value = { "channelses", "deliverieses", "attachmentses", "organization", "template", "createdBy" },
        allowSetters = true
    )
    private Message message;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public MessageChannel id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public MessageChannel channel(Channel channel) {
        this.setChannel(channel);
        return this;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Message getMessage() {
        return this.message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public MessageChannel message(Message message) {
        this.setMessage(message);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageChannel)) {
            return false;
        }
        return getId() != null && getId().equals(((MessageChannel) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageChannel{" +
            "id=" + getId() +
            ", channel='" + getChannel() + "'" +
            "}";
    }
}
