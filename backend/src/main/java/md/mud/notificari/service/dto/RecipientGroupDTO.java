package md.mud.notificari.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link md.mud.notificari.domain.RecipientGroup} entity.
 */
@Schema(description = "Grupurile se creeaza dinamic la importul CSV. Unic pe organizatie.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RecipientGroupDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 80)
    private String name;

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
        if (!(o instanceof RecipientGroupDTO)) {
            return false;
        }

        RecipientGroupDTO recipientGroupDTO = (RecipientGroupDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, recipientGroupDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RecipientGroupDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", organization=" + getOrganization() +
            "}";
    }
}
