package md.mud.notificari.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import md.mud.notificari.domain.MessageChannel;
import md.mud.notificari.repository.MessageChannelRepository;
import md.mud.notificari.service.dto.MessageChannelDTO;
import md.mud.notificari.service.mapper.MessageChannelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link md.mud.notificari.domain.MessageChannel}.
 */
@Service
@Transactional
public class MessageChannelService {

    private static final Logger LOG = LoggerFactory.getLogger(MessageChannelService.class);

    private final MessageChannelRepository messageChannelRepository;

    private final MessageChannelMapper messageChannelMapper;

    public MessageChannelService(MessageChannelRepository messageChannelRepository, MessageChannelMapper messageChannelMapper) {
        this.messageChannelRepository = messageChannelRepository;
        this.messageChannelMapper = messageChannelMapper;
    }

    /**
     * Save a messageChannel.
     *
     * @param messageChannelDTO the entity to save.
     * @return the persisted entity.
     */
    public MessageChannelDTO save(MessageChannelDTO messageChannelDTO) {
        LOG.debug("Request to save MessageChannel : {}", messageChannelDTO);
        MessageChannel messageChannel = messageChannelMapper.toEntity(messageChannelDTO);
        messageChannel = messageChannelRepository.save(messageChannel);
        return messageChannelMapper.toDto(messageChannel);
    }

    /**
     * Update a messageChannel.
     *
     * @param messageChannelDTO the entity to save.
     * @return the persisted entity.
     */
    public MessageChannelDTO update(MessageChannelDTO messageChannelDTO) {
        LOG.debug("Request to update MessageChannel : {}", messageChannelDTO);
        MessageChannel messageChannel = messageChannelMapper.toEntity(messageChannelDTO);
        messageChannel = messageChannelRepository.save(messageChannel);
        return messageChannelMapper.toDto(messageChannel);
    }

    /**
     * Partially update a messageChannel.
     *
     * @param messageChannelDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MessageChannelDTO> partialUpdate(MessageChannelDTO messageChannelDTO) {
        LOG.debug("Request to partially update MessageChannel : {}", messageChannelDTO);

        return messageChannelRepository
            .findById(messageChannelDTO.getId())
            .map(existingMessageChannel -> {
                messageChannelMapper.partialUpdate(existingMessageChannel, messageChannelDTO);

                return existingMessageChannel;
            })
            .map(messageChannelRepository::save)
            .map(messageChannelMapper::toDto);
    }

    /**
     * Get all the messageChannels.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<MessageChannelDTO> findAll() {
        LOG.debug("Request to get all MessageChannels");
        return messageChannelRepository
            .findAll()
            .stream()
            .map(messageChannelMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the messageChannels with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<MessageChannelDTO> findAllWithEagerRelationships(Pageable pageable) {
        return messageChannelRepository.findAllWithEagerRelationships(pageable).map(messageChannelMapper::toDto);
    }

    /**
     * Get one messageChannel by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MessageChannelDTO> findOne(Long id) {
        LOG.debug("Request to get MessageChannel : {}", id);
        return messageChannelRepository.findOneWithEagerRelationships(id).map(messageChannelMapper::toDto);
    }

    /**
     * Delete the messageChannel by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete MessageChannel : {}", id);
        messageChannelRepository.deleteById(id);
    }
}
