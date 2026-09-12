package md.mud.notificari.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import md.mud.notificari.domain.enumeration.Channel;
import md.mud.notificari.domain.enumeration.DeliveryStatus;

/**
 * O linie per (destinatar, canal) efectiv folosit la trimitere.
 * Aici se materializeaza si override-ul de canal per persoana din pasul 3.
 *
 * Tabelul e si coada de trimitere: dispecerul ia randurile PENDING/SENDING cu
 * next_attempt_at trecut, deci e scris cu UPDATE-uri in masa care ocolesc
 * cache-ul de nivel 2. Din acest motiv entitatea nu e cache-uita - vezi
 * CacheConfiguration.
 */
@Entity
@Table(name = "message_recipient")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageRecipient implements Serializable {

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
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliveryStatus status;

    @Size(max = 120)
    @Column(name = "provider_message_id", length = 120)
    private String providerMessageId;

    @Size(max = 500)
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    /** De cate ori livrarea a fost data unui provider. Creste la fiecare claim. */
    @NotNull
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    /**
     * Cand redevine disponibila pentru dispecer. Cat timp randul e SENDING, tot
     * acest camp tine termenul de vizibilitate: daca aplicatia moare la mijloc,
     * randul se reia singur dupa ce trece termenul, fara job de curatare.
     */
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "channelses", "organization", "recipientGroup" }, allowSetters = true)
    private Recipient recipient;

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

    public MessageRecipient id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public MessageRecipient channel(Channel channel) {
        this.setChannel(channel);
        return this;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public DeliveryStatus getStatus() {
        return this.status;
    }

    public MessageRecipient status(DeliveryStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public String getProviderMessageId() {
        return this.providerMessageId;
    }

    public MessageRecipient providerMessageId(String providerMessageId) {
        this.setProviderMessageId(providerMessageId);
        return this;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public MessageRecipient errorMessage(String errorMessage) {
        this.setErrorMessage(errorMessage);
        return this;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getSentAt() {
        return this.sentAt;
    }

    public MessageRecipient sentAt(Instant sentAt) {
        this.setSentAt(sentAt);
        return this;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getDeliveredAt() {
        return this.deliveredAt;
    }

    public MessageRecipient deliveredAt(Instant deliveredAt) {
        this.setDeliveredAt(deliveredAt);
        return this;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Integer getAttemptCount() {
        return this.attemptCount;
    }

    public MessageRecipient attemptCount(Integer attemptCount) {
        this.setAttemptCount(attemptCount);
        return this;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getNextAttemptAt() {
        return this.nextAttemptAt;
    }

    public MessageRecipient nextAttemptAt(Instant nextAttemptAt) {
        this.setNextAttemptAt(nextAttemptAt);
        return this;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public Instant getLastAttemptAt() {
        return this.lastAttemptAt;
    }

    public MessageRecipient lastAttemptAt(Instant lastAttemptAt) {
        this.setLastAttemptAt(lastAttemptAt);
        return this;
    }

    public void setLastAttemptAt(Instant lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public Recipient getRecipient() {
        return this.recipient;
    }

    public void setRecipient(Recipient recipient) {
        this.recipient = recipient;
    }

    public MessageRecipient recipient(Recipient recipient) {
        this.setRecipient(recipient);
        return this;
    }

    public Message getMessage() {
        return this.message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public MessageRecipient message(Message message) {
        this.setMessage(message);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageRecipient)) {
            return false;
        }
        return getId() != null && getId().equals(((MessageRecipient) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageRecipient{" +
            "id=" + getId() +
            ", channel='" + getChannel() + "'" +
            ", status='" + getStatus() + "'" +
            ", providerMessageId='" + getProviderMessageId() + "'" +
            ", errorMessage='" + getErrorMessage() + "'" +
            ", sentAt='" + getSentAt() + "'" +
            ", deliveredAt='" + getDeliveredAt() + "'" +
            ", attemptCount=" + getAttemptCount() +
            ", nextAttemptAt='" + getNextAttemptAt() + "'" +
            ", lastAttemptAt='" + getLastAttemptAt() + "'" +
            "}";
    }
}
