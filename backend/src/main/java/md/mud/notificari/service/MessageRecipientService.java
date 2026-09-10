package md.mud.notificari.service;

import java.util.Optional;
import md.mud.notificari.domain.MessageRecipient;
import md.mud.notificari.repository.MessageRecipientRepository;
import md.mud.notificari.service.dto.MessageRecipientDTO;
import md.mud.notificari.service.mapper.MessageRecipientMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link md.mud.notificari.domain.MessageRecipient}.
 */
@Service
@Transactional
public class MessageRecipientService {

    private static final Logger LOG = LoggerFactory.getLogger(MessageRecipientService.class);

    private final MessageRecipientRepository messageRecipientRepository;

    private final MessageRecipientMapper messageRecipientMapper;

    public MessageRecipientService(MessageRecipientRepository messageRecipientRepository, MessageRecipientMapper messageRecipientMapper) {
        this.messageRecipientRepository = messageRecipientRepository;
        this.messageRecipientMapper = messageRecipientMapper;
    }

    /**
     * Save a messageRecipient.
     *
     * @param messageRecipientDTO the entity to save.
     * @return the persisted entity.
     */
    public MessageRecipientDTO save(MessageRecipientDTO messageRecipientDTO) {
        LOG.debug("Request to save MessageRecipient : {}", messageRecipientDTO);
        MessageRecipient messageRecipient = messageRecipientMapper.toEntity(messageRecipientDTO);
        messageRecipient = messageRecipientRepository.save(messageRecipient);
        return messageRecipientMapper.toDto(messageRecipient);
    }

    /**
     * Update a messageRecipient.
     *
     * @param messageRecipientDTO the entity to save.
     * @return the persisted entity.
     */
    public MessageRecipientDTO update(MessageRecipientDTO messageRecipientDTO) {
        LOG.debug("Request to update MessageRecipient : {}", messageRecipientDTO);
        MessageRecipient messageRecipient = messageRecipientMapper.toEntity(messageRecipientDTO);
        messageRecipient = messageRecipientRepository.save(messageRecipient);
        return messageRecipientMapper.toDto(messageRecipient);
    }

    /**
     * Partially update a messageRecipient.
     *
     * @param messageRecipientDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MessageRecipientDTO> partialUpdate(MessageRecipientDTO messageRecipientDTO) {
        LOG.debug("Request to partially update MessageRecipient : {}", messageRecipientDTO);

        return messageRecipientRepository
            .findById(messageRecipientDTO.getId())
            .map(existingMessageRecipient -> {
                messageRecipientMapper.partialUpdate(existingMessageRecipient, messageRecipientDTO);

                return existingMessageRecipient;
            })
            .map(messageRecipientRepository::save)
            .map(messageRecipientMapper::toDto);
    }

    /**
     * Get all the messageRecipients.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<MessageRecipientDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all MessageRecipients");
        return messageRecipientRepository.findAll(pageable).map(messageRecipientMapper::toDto);
    }

    /**
     * Get all the messageRecipients with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<MessageRecipientDTO> findAllWithEagerRelationships(Pageable pageable) {
        return messageRecipientRepository.findAllWithEagerRelationships(pageable).map(messageRecipientMapper::toDto);
    }

    /**
     * Get one messageRecipient by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MessageRecipientDTO> findOne(Long id) {
        LOG.debug("Request to get MessageRecipient : {}", id);
        return messageRecipientRepository.findOneWithEagerRelationships(id).map(messageRecipientMapper::toDto);
    }

    /**
     * Delete the messageRecipient by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete MessageRecipient : {}", id);
        messageRecipientRepository.deleteById(id);
    }
}
