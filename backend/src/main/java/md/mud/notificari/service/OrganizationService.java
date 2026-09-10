package md.mud.notificari.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import md.mud.notificari.domain.Organization;
import md.mud.notificari.repository.OrganizationRepository;
import md.mud.notificari.service.dto.OrganizationDTO;
import md.mud.notificari.service.mapper.OrganizationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link md.mud.notificari.domain.Organization}.
 */
@Service
@Transactional
public class OrganizationService {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository organizationRepository;

    private final OrganizationMapper organizationMapper;

    public OrganizationService(OrganizationRepository organizationRepository, OrganizationMapper organizationMapper) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
    }

    /**
     * Save a organization.
     *
     * @param organizationDTO the entity to save.
     * @return the persisted entity.
     */
    public OrganizationDTO save(OrganizationDTO organizationDTO) {
        LOG.debug("Request to save Organization : {}", organizationDTO);
        Organization organization = organizationMapper.toEntity(organizationDTO);
        organization = organizationRepository.save(organization);
        return organizationMapper.toDto(organization);
    }

    /**
     * Update a organization.
     *
     * @param organizationDTO the entity to save.
     * @return the persisted entity.
     */
    public OrganizationDTO update(OrganizationDTO organizationDTO) {
        LOG.debug("Request to update Organization : {}", organizationDTO);
        Organization organization = organizationMapper.toEntity(organizationDTO);
        organization = organizationRepository.save(organization);
        return organizationMapper.toDto(organization);
    }

    /**
     * Partially update a organization.
     *
     * @param organizationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OrganizationDTO> partialUpdate(OrganizationDTO organizationDTO) {
        LOG.debug("Request to partially update Organization : {}", organizationDTO);

        return organizationRepository
            .findById(organizationDTO.getId())
            .map(existingOrganization -> {
                organizationMapper.partialUpdate(existingOrganization, organizationDTO);

                return existingOrganization;
            })
            .map(organizationRepository::save)
            .map(organizationMapper::toDto);
    }

    /**
     * Get all the organizations.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<OrganizationDTO> findAll() {
        LOG.debug("Request to get all Organizations");
        return organizationRepository.findAll().stream().map(organizationMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one organization by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OrganizationDTO> findOne(Long id) {
        LOG.debug("Request to get Organization : {}", id);
        return organizationRepository.findById(id).map(organizationMapper::toDto);
    }

    /**
     * Delete the organization by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Organization : {}", id);
        organizationRepository.deleteById(id);
    }
}
