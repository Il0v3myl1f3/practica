package md.mud.notificari.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import md.mud.notificari.domain.RecipientChannel;
import md.mud.notificari.repository.RecipientChannelRepository;
import md.mud.notificari.service.dto.RecipientChannelDTO;
import md.mud.notificari.service.mapper.RecipientChannelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link md.mud.notificari.domain.RecipientChannel}.
 */
@Service
@Transactional
public class RecipientChannelService {

    private static final Logger LOG = LoggerFactory.getLogger(RecipientChannelService.class);

    private final RecipientChannelRepository recipientChannelRepository;

    private final RecipientChannelMapper recipientChannelMapper;

    public RecipientChannelService(RecipientChannelRepository recipientChannelRepository, RecipientChannelMapper recipientChannelMapper) {
        this.recipientChannelRepository = recipientChannelRepository;
        this.recipientChannelMapper = recipientChannelMapper;
    }

    /**
     * Save a recipientChannel.
     *
     * @param recipientChannelDTO the entity to save.
     * @return the persisted entity.
     */
    public RecipientChannelDTO save(RecipientChannelDTO recipientChannelDTO) {
        LOG.debug("Request to save RecipientChannel : {}", recipientChannelDTO);
        RecipientChannel recipientChannel = recipientChannelMapper.toEntity(recipientChannelDTO);
        recipientChannel = recipientChannelRepository.save(recipientChannel);
        return recipientChannelMapper.toDto(recipientChannel);
    }

    /**
     * Update a recipientChannel.
     *
     * @param recipientChannelDTO the entity to save.
     * @return the persisted entity.
     */
    public RecipientChannelDTO update(RecipientChannelDTO recipientChannelDTO) {
        LOG.debug("Request to update RecipientChannel : {}", recipientChannelDTO);
        RecipientChannel recipientChannel = recipientChannelMapper.toEntity(recipientChannelDTO);
        recipientChannel = recipientChannelRepository.save(recipientChannel);
        return recipientChannelMapper.toDto(recipientChannel);
    }

    /**
     * Partially update a recipientChannel.
     *
     * @param recipientChannelDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RecipientChannelDTO> partialUpdate(RecipientChannelDTO recipientChannelDTO) {
        LOG.debug("Request to partially update RecipientChannel : {}", recipientChannelDTO);

        return recipientChannelRepository
            .findById(recipientChannelDTO.getId())
            .map(existingRecipientChannel -> {
                recipientChannelMapper.partialUpdate(existingRecipientChannel, recipientChannelDTO);

                return existingRecipientChannel;
            })
            .map(recipientChannelRepository::save)
            .map(recipientChannelMapper::toDto);
    }

    /**
     * Get all the recipientChannels.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<RecipientChannelDTO> findAll() {
        LOG.debug("Request to get all RecipientChannels");
        return recipientChannelRepository
            .findAll()
            .stream()
            .map(recipientChannelMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the recipientChannels with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RecipientChannelDTO> findAllWithEagerRelationships(Pageable pageable) {
        return recipientChannelRepository.findAllWithEagerRelationships(pageable).map(recipientChannelMapper::toDto);
    }

    /**
     * Get one recipientChannel by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RecipientChannelDTO> findOne(Long id) {
        LOG.debug("Request to get RecipientChannel : {}", id);
        return recipientChannelRepository.findOneWithEagerRelationships(id).map(recipientChannelMapper::toDto);
    }

    /**
     * Delete the recipientChannel by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete RecipientChannel : {}", id);
        recipientChannelRepository.deleteById(id);
    }
}
