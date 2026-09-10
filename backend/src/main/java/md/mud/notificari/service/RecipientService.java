package md.mud.notificari.service;

import java.util.Optional;
import md.mud.notificari.domain.Recipient;
import md.mud.notificari.repository.RecipientRepository;
import md.mud.notificari.service.dto.RecipientDTO;
import md.mud.notificari.service.mapper.RecipientMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link md.mud.notificari.domain.Recipient}.
 */
@Service
@Transactional
public class RecipientService {

    private static final Logger LOG = LoggerFactory.getLogger(RecipientService.class);

    private final RecipientRepository recipientRepository;

    private final RecipientMapper recipientMapper;

    public RecipientService(RecipientRepository recipientRepository, RecipientMapper recipientMapper) {
        this.recipientRepository = recipientRepository;
        this.recipientMapper = recipientMapper;
    }

    /**
     * Save a recipient.
     *
     * @param recipientDTO the entity to save.
     * @return the persisted entity.
     */
    public RecipientDTO save(RecipientDTO recipientDTO) {
        LOG.debug("Request to save Recipient : {}", recipientDTO);
        Recipient recipient = recipientMapper.toEntity(recipientDTO);
        recipient = recipientRepository.save(recipient);
        return recipientMapper.toDto(recipient);
    }

    /**
     * Update a recipient.
     *
     * @param recipientDTO the entity to save.
     * @return the persisted entity.
     */
    public RecipientDTO update(RecipientDTO recipientDTO) {
        LOG.debug("Request to update Recipient : {}", recipientDTO);
        Recipient recipient = recipientMapper.toEntity(recipientDTO);
        recipient = recipientRepository.save(recipient);
        return recipientMapper.toDto(recipient);
    }

    /**
     * Partially update a recipient.
     *
     * @param recipientDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RecipientDTO> partialUpdate(RecipientDTO recipientDTO) {
        LOG.debug("Request to partially update Recipient : {}", recipientDTO);

        return recipientRepository
            .findById(recipientDTO.getId())
            .map(existingRecipient -> {
                recipientMapper.partialUpdate(existingRecipient, recipientDTO);

                return existingRecipient;
            })
            .map(recipientRepository::save)
            .map(recipientMapper::toDto);
    }

    /**
     * Get all the recipients with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RecipientDTO> findAllWithEagerRelationships(Pageable pageable) {
        return recipientRepository.findAllWithEagerRelationships(pageable).map(recipientMapper::toDto);
    }

    /**
     * Get one recipient by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RecipientDTO> findOne(Long id) {
        LOG.debug("Request to get Recipient : {}", id);
        return recipientRepository.findOneWithEagerRelationships(id).map(recipientMapper::toDto);
    }

    /**
     * Delete the recipient by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Recipient : {}", id);
        recipientRepository.deleteById(id);
    }
}
