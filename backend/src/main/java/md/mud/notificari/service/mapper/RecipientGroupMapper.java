package md.mud.notificari.service.mapper;

import md.mud.notificari.domain.Organization;
import md.mud.notificari.domain.RecipientGroup;
import md.mud.notificari.service.dto.OrganizationDTO;
import md.mud.notificari.service.dto.RecipientGroupDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RecipientGroup} and its DTO {@link RecipientGroupDTO}.
 */
@Mapper(componentModel = "spring")
public interface RecipientGroupMapper extends EntityMapper<RecipientGroupDTO, RecipientGroup> {
    @Mapping(target = "organization", source = "organization", qualifiedByName = "organizationName")
    RecipientGroupDTO toDto(RecipientGroup s);

    @Named("organizationName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    OrganizationDTO toDtoOrganizationName(Organization organization);
}
