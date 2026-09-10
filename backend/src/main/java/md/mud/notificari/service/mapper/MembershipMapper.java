package md.mud.notificari.service.mapper;

import md.mud.notificari.domain.Membership;
import md.mud.notificari.domain.Organization;
import md.mud.notificari.domain.User;
import md.mud.notificari.service.dto.MembershipDTO;
import md.mud.notificari.service.dto.OrganizationDTO;
import md.mud.notificari.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Membership} and its DTO {@link MembershipDTO}.
 */
@Mapper(componentModel = "spring")
public interface MembershipMapper extends EntityMapper<MembershipDTO, Membership> {
    @Mapping(target = "user", source = "user", qualifiedByName = "userLogin")
    @Mapping(target = "organization", source = "organization", qualifiedByName = "organizationName")
    MembershipDTO toDto(Membership s);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);

    @Named("organizationName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    OrganizationDTO toDtoOrganizationName(Organization organization);
}
