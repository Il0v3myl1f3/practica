package md.mud.notificari.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import md.mud.notificari.domain.MessageAttachment;
import md.mud.notificari.repository.MessageAttachmentRepository;
import md.mud.notificari.service.dto.MessageAttachmentDTO;
import md.mud.notificari.service.mapper.MessageAttachmentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link md.mud.notificari.domain.MessageAttachment}.
 */
@Service
@Transactional
public class MessageAttachmentService {

    private static final Logger LOG = LoggerFactory.getLogger(MessageAttachmentService.class);

    private final MessageAttachmentRepository messageAttachmentRepository;

    private final MessageAttachmentMapper messageAttachmentMapper;

    public MessageAttachmentService(
        MessageAttachmentRepository messageAttachmentRepository,
        MessageAttachmentMapper messageAttachmentMapper
    ) {
        this.messageAttachmentRepository = messageAttachmentRepository;
        this.messageAttachmentMapper = messageAttachmentMapper;
    }

    /**
     * Save a messageAttachment.
     *
     * @param messageAttachmentDTO the entity to save.
     * @return the persisted entity.
     */
    public MessageAttachmentDTO save(MessageAttachmentDTO messageAttachmentDTO) {
        LOG.debug("Request to save MessageAttachment : {}", messageAttachmentDTO);
        MessageAttachment messageAttachment = messageAttachmentMapper.toEntity(messageAttachmentDTO);
        messageAttachment = messageAttachmentRepository.save(messageAttachment);
        return messageAttachmentMapper.toDto(messageAttachment);
    }

    /**
     * Update a messageAttachment.
     *
     * @param messageAttachmentDTO the entity to save.
     * @return the persisted entity.
     */
    public MessageAttachmentDTO update(MessageAttachmentDTO messageAttachmentDTO) {
        LOG.debug("Request to update MessageAttachment : {}", messageAttachmentDTO);
        MessageAttachment messageAttachment = messageAttachmentMapper.toEntity(messageAttachmentDTO);
        messageAttachment = messageAttachmentRepository.save(messageAttachment);
        return messageAttachmentMapper.toDto(messageAttachment);
    }

    /**
     * Partially update a messageAttachment.
     *
     * @param messageAttachmentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MessageAttachmentDTO> partialUpdate(MessageAttachmentDTO messageAttachmentDTO) {
        LOG.debug("Request to partially update MessageAttachment : {}", messageAttachmentDTO);

        return messageAttachmentRepository
            .findById(messageAttachmentDTO.getId())
            .map(existingMessageAttachment -> {
                messageAttachmentMapper.partialUpdate(existingMessageAttachment, messageAttachmentDTO);

                return existingMessageAttachment;
            })
            .map(messageAttachmentRepository::save)
            .map(messageAttachmentMapper::toDto);
    }

    /**
     * Get all the messageAttachments.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<MessageAttachmentDTO> findAll() {
        LOG.debug("Request to get all MessageAttachments");
        return messageAttachmentRepository
            .findAll()
            .stream()
            .map(messageAttachmentMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the messageAttachments with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<MessageAttachmentDTO> findAllWithEagerRelationships(Pageable pageable) {
        return messageAttachmentRepository.findAllWithEagerRelationships(pageable).map(messageAttachmentMapper::toDto);
    }

    /**
     * Get one messageAttachment by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MessageAttachmentDTO> findOne(Long id) {
        LOG.debug("Request to get MessageAttachment : {}", id);
        return messageAttachmentRepository.findOneWithEagerRelationships(id).map(messageAttachmentMapper::toDto);
    }

    /**
     * Delete the messageAttachment by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete MessageAttachment : {}", id);
        messageAttachmentRepository.deleteById(id);
    }
}
