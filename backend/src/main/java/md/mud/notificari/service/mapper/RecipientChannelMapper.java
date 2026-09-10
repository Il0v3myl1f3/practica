package md.mud.notificari.service.mapper;

import md.mud.notificari.domain.Recipient;
import md.mud.notificari.domain.RecipientChannel;
import md.mud.notificari.service.dto.RecipientChannelDTO;
import md.mud.notificari.service.dto.RecipientDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RecipientChannel} and its DTO {@link RecipientChannelDTO}.
 */
@Mapper(componentModel = "spring")
public interface RecipientChannelMapper extends EntityMapper<RecipientChannelDTO, RecipientChannel> {
    @Mapping(target = "recipient", source = "recipient", qualifiedByName = "recipientEmail")
    RecipientChannelDTO toDto(RecipientChannel s);

    @Named("recipientEmail")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "email", source = "email")
    RecipientDTO toDtoRecipientEmail(Recipient recipient);
}
