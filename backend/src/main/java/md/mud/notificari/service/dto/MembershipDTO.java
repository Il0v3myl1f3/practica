package md.mud.notificari.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import md.mud.notificari.domain.enumeration.MemberRole;

/**
 * A DTO for the {@link md.mud.notificari.domain.Membership} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MembershipDTO implements Serializable {

    private Long id;

    @NotNull
    private MemberRole role;

    @NotNull
    private UserDTO user;

    @NotNull
    private OrganizationDTO organization;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
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
        if (!(o instanceof MembershipDTO)) {
            return false;
        }

        MembershipDTO membershipDTO = (MembershipDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, membershipDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MembershipDTO{" +
            "id=" + getId() +
            ", role='" + getRole() + "'" +
            ", user=" + getUser() +
            ", organization=" + getOrganization() +
            "}";
    }
}
