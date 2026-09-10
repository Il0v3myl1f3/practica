package md.mud.notificari.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import md.mud.notificari.domain.RecipientGroup;
import md.mud.notificari.repository.RecipientGroupRepository;
import md.mud.notificari.service.dto.RecipientGroupDTO;
import md.mud.notificari.service.mapper.RecipientGroupMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link md.mud.notificari.domain.RecipientGroup}.
 */
@Service
@Transactional
public class RecipientGroupService {

    private static final Logger LOG = LoggerFactory.getLogger(RecipientGroupService.class);

    private final RecipientGroupRepository recipientGroupRepository;

    private final RecipientGroupMapper recipientGroupMapper;

    public RecipientGroupService(RecipientGroupRepository recipientGroupRepository, RecipientGroupMapper recipientGroupMapper) {
        this.recipientGroupRepository = recipientGroupRepository;
        this.recipientGroupMapper = recipientGroupMapper;
    }

    /**
     * Save a recipientGroup.
     *
     * @param recipientGroupDTO the entity to save.
     * @return the persisted entity.
     */
    public RecipientGroupDTO save(RecipientGroupDTO recipientGroupDTO) {
        LOG.debug("Request to save RecipientGroup : {}", recipientGroupDTO);
        RecipientGroup recipientGroup = recipientGroupMapper.toEntity(recipientGroupDTO);
        recipientGroup = recipientGroupRepository.save(recipientGroup);
        return recipientGroupMapper.toDto(recipientGroup);
    }

    /**
     * Update a recipientGroup.
     *
     * @param recipientGroupDTO the entity to save.
     * @return the persisted entity.
     */
    public RecipientGroupDTO update(RecipientGroupDTO recipientGroupDTO) {
        LOG.debug("Request to update RecipientGroup : {}", recipientGroupDTO);
        RecipientGroup recipientGroup = recipientGroupMapper.toEntity(recipientGroupDTO);
        recipientGroup = recipientGroupRepository.save(recipientGroup);
        return recipientGroupMapper.toDto(recipientGroup);
    }

    /**
     * Partially update a recipientGroup.
     *
     * @param recipientGroupDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RecipientGroupDTO> partialUpdate(RecipientGroupDTO recipientGroupDTO) {
        LOG.debug("Request to partially update RecipientGroup : {}", recipientGroupDTO);

        return recipientGroupRepository
            .findById(recipientGroupDTO.getId())
            .map(existingRecipientGroup -> {
                recipientGroupMapper.partialUpdate(existingRecipientGroup, recipientGroupDTO);

                return existingRecipientGroup;
            })
            .map(recipientGroupRepository::save)
            .map(recipientGroupMapper::toDto);
    }

    /**
     * Get all the recipientGroups.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<RecipientGroupDTO> findAll() {
        LOG.debug("Request to get all RecipientGroups");
        return recipientGroupRepository
            .findAll()
            .stream()
            .map(recipientGroupMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the recipientGroups with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RecipientGroupDTO> findAllWithEagerRelationships(Pageable pageable) {
        return recipientGroupRepository.findAllWithEagerRelationships(pageable).map(recipientGroupMapper::toDto);
    }

    /**
     * Get one recipientGroup by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RecipientGroupDTO> findOne(Long id) {
        LOG.debug("Request to get RecipientGroup : {}", id);
        return recipientGroupRepository.findOneWithEagerRelationships(id).map(recipientGroupMapper::toDto);
    }

    /**
     * Delete the recipientGroup by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete RecipientGroup : {}", id);
        recipientGroupRepository.deleteById(id);
    }
}
