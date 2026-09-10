package md.mud.notificari.service.mapper;

import md.mud.notificari.domain.Message;
import md.mud.notificari.domain.MessageRecipient;
import md.mud.notificari.domain.Recipient;
import md.mud.notificari.service.dto.MessageDTO;
import md.mud.notificari.service.dto.MessageRecipientDTO;
import md.mud.notificari.service.dto.RecipientDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MessageRecipient} and its DTO {@link MessageRecipientDTO}.
 */
@Mapper(componentModel = "spring")
public interface MessageRecipientMapper extends EntityMapper<MessageRecipientDTO, MessageRecipient> {
    @Mapping(target = "recipient", source = "recipient", qualifiedByName = "recipientEmail")
    @Mapping(target = "message", source = "message", qualifiedByName = "messageSubject")
    MessageRecipientDTO toDto(MessageRecipient s);

    @Named("recipientEmail")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "email", source = "email")
    RecipientDTO toDtoRecipientEmail(Recipient recipient);

    @Named("messageSubject")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "subject", source = "subject")
    MessageDTO toDtoMessageSubject(Message message);
}
