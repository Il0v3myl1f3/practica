package md.mud.notificari.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import md.mud.notificari.domain.enumeration.MessageStatus;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Ciorna si trimitere in aceeasi entitate, diferentiate prin status.
 * bodyHtml contine chip-uri de variabile: {{nume}} {{prenume}} {{grup}} {{email}}
 */
@Entity
@Table(name = "message")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Size(max = 200)
    @Column(name = "subject", length = 200)
    private String subject;

    @Lob
    @Column(name = "body_html")
    private String bodyHtml;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MessageStatus status;

    @Min(value = 0)
    @Column(name = "recipient_count")
    private Integer recipientCount;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "message")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "message" }, allowSetters = true)
    private Set<MessageChannel> channelses = new HashSet<>();

    // Fara cache de nivel 2: livrarile sunt coada de trimitere, scrisa cu UPDATE-uri
    // in masa de catre dispecer, care nu trec prin cache.
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "message")
    @JsonIgnoreProperties(value = { "recipient", "message" }, allowSetters = true)
    private Set<MessageRecipient> deliverieses = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "message")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "message" }, allowSetters = true)
    private Set<MessageAttachment> attachmentses = new HashSet<>();

    @ManyToOne(optional = false)
    @NotNull
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "organization" }, allowSetters = true)
    private MessageTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    private User createdBy;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Message id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return this.subject;
    }

    public Message subject(String subject) {
        this.setSubject(subject);
        return this;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodyHtml() {
        return this.bodyHtml;
    }

    public Message bodyHtml(String bodyHtml) {
        this.setBodyHtml(bodyHtml);
        return this;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public MessageStatus getStatus() {
        return this.status;
    }

    public Message status(MessageStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public Integer getRecipientCount() {
        return this.recipientCount;
    }

    public Message recipientCount(Integer recipientCount) {
        this.setRecipientCount(recipientCount);
        return this;
    }

    public void setRecipientCount(Integer recipientCount) {
        this.recipientCount = recipientCount;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Message createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getSentAt() {
        return this.sentAt;
    }

    public Message sentAt(Instant sentAt) {
        this.setSentAt(sentAt);
        return this;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Set<MessageChannel> getChannelses() {
        return this.channelses;
    }

    public void setChannelses(Set<MessageChannel> messageChannels) {
        if (this.channelses != null) {
            this.channelses.forEach(i -> i.setMessage(null));
        }
        if (messageChannels != null) {
            messageChannels.forEach(i -> i.setMessage(this));
        }
        this.channelses = messageChannels;
    }

    public Message channelses(Set<MessageChannel> messageChannels) {
        this.setChannelses(messageChannels);
        return this;
    }

    public Message addChannels(MessageChannel messageChannel) {
        this.channelses.add(messageChannel);
        messageChannel.setMessage(this);
        return this;
    }

    public Message removeChannels(MessageChannel messageChannel) {
        this.channelses.remove(messageChannel);
        messageChannel.setMessage(null);
        return this;
    }

    public Set<MessageRecipient> getDeliverieses() {
        return this.deliverieses;
    }

    public void setDeliverieses(Set<MessageRecipient> messageRecipients) {
        if (this.deliverieses != null) {
            this.deliverieses.forEach(i -> i.setMessage(null));
        }
        if (messageRecipients != null) {
            messageRecipients.forEach(i -> i.setMessage(this));
        }
        this.deliverieses = messageRecipients;
    }

    public Message deliverieses(Set<MessageRecipient> messageRecipients) {
        this.setDeliverieses(messageRecipients);
        return this;
    }

    public Message addDeliveries(MessageRecipient messageRecipient) {
        this.deliverieses.add(messageRecipient);
        messageRecipient.setMessage(this);
        return this;
    }

    public Message removeDeliveries(MessageRecipient messageRecipient) {
        this.deliverieses.remove(messageRecipient);
        messageRecipient.setMessage(null);
        return this;
    }

    public Set<MessageAttachment> getAttachmentses() {
        return this.attachmentses;
    }

    public void setAttachmentses(Set<MessageAttachment> messageAttachments) {
        if (this.attachmentses != null) {
            this.attachmentses.forEach(i -> i.setMessage(null));
        }
        if (messageAttachments != null) {
            messageAttachments.forEach(i -> i.setMessage(this));
        }
        this.attachmentses = messageAttachments;
    }

    public Message attachmentses(Set<MessageAttachment> messageAttachments) {
        this.setAttachmentses(messageAttachments);
        return this;
    }

    public Message addAttachments(MessageAttachment messageAttachment) {
        this.attachmentses.add(messageAttachment);
        messageAttachment.setMessage(this);
        return this;
    }

    public Message removeAttachments(MessageAttachment messageAttachment) {
        this.attachmentses.remove(messageAttachment);
        messageAttachment.setMessage(null);
        return this;
    }

    public Organization getOrganization() {
        return this.organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Message organization(Organization organization) {
        this.setOrganization(organization);
        return this;
    }

    public MessageTemplate getTemplate() {
        return this.template;
    }

    public void setTemplate(MessageTemplate messageTemplate) {
        this.template = messageTemplate;
    }

    public Message template(MessageTemplate messageTemplate) {
        this.setTemplate(messageTemplate);
        return this;
    }

    public User getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(User user) {
        this.createdBy = user;
    }

    public Message createdBy(User user) {
        this.setCreatedBy(user);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Message)) {
            return false;
        }
        return getId() != null && getId().equals(((Message) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Message{" +
            "id=" + getId() +
            ", subject='" + getSubject() + "'" +
            ", bodyHtml='" + getBodyHtml() + "'" +
            ", status='" + getStatus() + "'" +
            ", recipientCount=" + getRecipientCount() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", sentAt='" + getSentAt() + "'" +
            "}";
    }
}
