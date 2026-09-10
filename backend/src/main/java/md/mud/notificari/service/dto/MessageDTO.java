package md.mud.notificari.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import md.mud.notificari.domain.enumeration.MessageStatus;

/**
 * A DTO for the {@link md.mud.notificari.domain.Message} entity.
 */
@Schema(
    description = "Ciorna si trimitere in aceeasi entitate, diferentiate prin status.\nbodyHtml contine chip-uri de variabile: {{nume}} {{prenume}} {{grup}} {{email}}"
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageDTO implements Serializable {

    private Long id;

    @Size(max = 200)
    private String subject;

    @Lob
    private String bodyHtml;

    @NotNull
    private MessageStatus status;

    @Min(value = 0)
    private Integer recipientCount;

    @NotNull
    private Instant createdAt;

    private Instant sentAt;

    @NotNull
    private OrganizationDTO organization;

    private MessageTemplateDTO template;

    private UserDTO createdBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public Integer getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(Integer recipientCount) {
        this.recipientCount = recipientCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public OrganizationDTO getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationDTO organization) {
        this.organization = organization;
    }

    public MessageTemplateDTO getTemplate() {
        return template;
    }

    public void setTemplate(MessageTemplateDTO template) {
        this.template = template;
    }

    public UserDTO getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserDTO createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageDTO)) {
            return false;
        }

        MessageDTO messageDTO = (MessageDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, messageDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageDTO{" +
            "id=" + getId() +
            ", subject='" + getSubject() + "'" +
            ", bodyHtml='" + getBodyHtml() + "'" +
            ", status='" + getStatus() + "'" +
            ", recipientCount=" + getRecipientCount() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", sentAt='" + getSentAt() + "'" +
            ", organization=" + getOrganization() +
            ", template=" + getTemplate() +
            ", createdBy=" + getCreatedBy() +
            "}";
    }
}
