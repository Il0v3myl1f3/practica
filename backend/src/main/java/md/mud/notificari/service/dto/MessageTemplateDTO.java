package md.mud.notificari.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link md.mud.notificari.domain.MessageTemplate} entity.
 */
@Schema(description = "Daca subject lipseste la salvare, se completeaza cu name.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageTemplateDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 120)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 200)
    private String subject;

    @Lob
    private String body;

    @NotNull
    private OrganizationDTO organization;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public OrganizationDTO getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationDTO organization) {
        this.organization = organization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageTemplateDTO)) {
            return false;
        }

        MessageTemplateDTO messageTemplateDTO = (MessageTemplateDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, messageTemplateDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageTemplateDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", subject='" + getSubject() + "'" +
            ", body='" + getBody() + "'" +
            ", organization=" + getOrganization() +
            "}";
    }
}
