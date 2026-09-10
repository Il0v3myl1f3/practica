package md.mud.notificari.service.mapper;

import md.mud.notificari.domain.Message;
import md.mud.notificari.domain.MessageChannel;
import md.mud.notificari.service.dto.MessageChannelDTO;
import md.mud.notificari.service.dto.MessageDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MessageChannel} and its DTO {@link MessageChannelDTO}.
 */
@Mapper(componentModel = "spring")
public interface MessageChannelMapper extends EntityMapper<MessageChannelDTO, MessageChannel> {
    @Mapping(target = "message", source = "message", qualifiedByName = "messageSubject")
    MessageChannelDTO toDto(MessageChannel s);

    @Named("messageSubject")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "subject", source = "subject")
    MessageDTO toDtoMessageSubject(Message message);
}
