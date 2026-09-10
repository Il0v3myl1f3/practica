package md.mud.notificari.service.mapper;

import md.mud.notificari.domain.Organization;
import md.mud.notificari.domain.Recipient;
import md.mud.notificari.domain.RecipientGroup;
import md.mud.notificari.service.dto.OrganizationDTO;
import md.mud.notificari.service.dto.RecipientDTO;
import md.mud.notificari.service.dto.RecipientGroupDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Recipient} and its DTO {@link RecipientDTO}.
 */
@Mapper(componentModel = "spring")
public interface RecipientMapper extends EntityMapper<RecipientDTO, Recipient> {
    @Mapping(target = "organization", source = "organization", qualifiedByName = "organizationName")
    @Mapping(target = "recipientGroup", source = "recipientGroup", qualifiedByName = "recipientGroupName")
    RecipientDTO toDto(Recipient s);

    @Named("organizationName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    OrganizationDTO toDtoOrganizationName(Organization organization);

    @Named("recipientGroupName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    RecipientGroupDTO toDtoRecipientGroupName(RecipientGroup recipientGroup);
}
