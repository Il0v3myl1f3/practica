package md.mud.notificari.service.mapper;

import md.mud.notificari.domain.MessageTemplate;
import md.mud.notificari.domain.Organization;
import md.mud.notificari.service.dto.MessageTemplateDTO;
import md.mud.notificari.service.dto.OrganizationDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MessageTemplate} and its DTO {@link MessageTemplateDTO}.
 */
@Mapper(componentModel = "spring")
public interface MessageTemplateMapper extends EntityMapper<MessageTemplateDTO, MessageTemplate> {
    @Mapping(target = "organization", source = "organization", qualifiedByName = "organizationName")
    MessageTemplateDTO toDto(MessageTemplate s);

    @Named("organizationName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    OrganizationDTO toDtoOrganizationName(Organization organization);
}
