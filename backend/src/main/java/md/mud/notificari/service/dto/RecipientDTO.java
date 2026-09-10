package md.mud.notificari.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link md.mud.notificari.domain.Recipient} entity.
 */
@Schema(description = "fullName NU se stocheaza: este derivat din firstName + \" \" + lastName.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RecipientDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 60)
    private String firstName;

    @NotNull
    @Size(max = 60)
    private String lastName;

    @NotNull
    @Size(max = 254)
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private String email;

    private Instant createdAt;

    @NotNull
    private OrganizationDTO organization;

    @NotNull
    private RecipientGroupDTO recipientGroup;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public OrganizationDTO getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationDTO organization) {
        this.organization = organization;
    }

    public RecipientGroupDTO getRecipientGroup() {
        return recipientGroup;
    }

    public void setRecipientGroup(RecipientGroupDTO recipientGroup) {
        this.recipientGroup = recipientGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecipientDTO)) {
            return false;
        }

        RecipientDTO recipientDTO = (RecipientDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, recipientDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RecipientDTO{" +
            "id=" + getId() +
            ", firstName='" + getFirstName() + "'" +
            ", lastName='" + getLastName() + "'" +
            ", email='" + getEmail() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", organization=" + getOrganization() +
            ", recipientGroup=" + getRecipientGroup() +
            "}";
    }
}
