package md.mud.notificari.service.mapper;

import md.mud.notificari.domain.Message;
import md.mud.notificari.domain.MessageAttachment;
import md.mud.notificari.service.dto.MessageAttachmentDTO;
import md.mud.notificari.service.dto.MessageDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MessageAttachment} and its DTO {@link MessageAttachmentDTO}.
 */
@Mapper(componentModel = "spring")
public interface MessageAttachmentMapper extends EntityMapper<MessageAttachmentDTO, MessageAttachment> {
    @Mapping(target = "message", source = "message", qualifiedByName = "messageSubject")
    MessageAttachmentDTO toDto(MessageAttachment s);

    @Named("messageSubject")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "subject", source = "subject")
    MessageDTO toDtoMessageSubject(Message message);
}
