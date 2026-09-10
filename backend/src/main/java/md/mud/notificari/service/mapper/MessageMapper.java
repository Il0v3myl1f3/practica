package md.mud.notificari.service.mapper;

import md.mud.notificari.domain.Message;
import md.mud.notificari.domain.MessageTemplate;
import md.mud.notificari.domain.Organization;
import md.mud.notificari.domain.User;
import md.mud.notificari.service.dto.MessageDTO;
import md.mud.notificari.service.dto.MessageTemplateDTO;
import md.mud.notificari.service.dto.OrganizationDTO;
import md.mud.notificari.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Message} and its DTO {@link MessageDTO}.
 */
@Mapper(componentModel = "spring")
public interface MessageMapper extends EntityMapper<MessageDTO, Message> {
    @Mapping(target = "organization", source = "organization", qualifiedByName = "organizationName")
    @Mapping(target = "template", source = "template", qualifiedByName = "messageTemplateName")
    @Mapping(target = "createdBy", source = "createdBy", qualifiedByName = "userLogin")
    MessageDTO toDto(Message s);

    @Named("organizationName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    OrganizationDTO toDtoOrganizationName(Organization organization);

    @Named("messageTemplateName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    MessageTemplateDTO toDtoMessageTemplateName(MessageTemplate messageTemplate);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
