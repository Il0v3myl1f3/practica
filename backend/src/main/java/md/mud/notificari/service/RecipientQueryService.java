package md.mud.notificari.service;

import jakarta.persistence.criteria.JoinType;
import md.mud.notificari.domain.*; // for static metamodels
import md.mud.notificari.domain.Recipient;
import md.mud.notificari.repository.RecipientRepository;
import md.mud.notificari.service.criteria.RecipientCriteria;
import md.mud.notificari.service.dto.RecipientDTO;
import md.mud.notificari.service.mapper.RecipientMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Recipient} entities in the database.
 * The main input is a {@link RecipientCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link RecipientDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class RecipientQueryService extends QueryService<Recipient> {

    private static final Logger LOG = LoggerFactory.getLogger(RecipientQueryService.class);

    private final RecipientRepository recipientRepository;

    private final RecipientMapper recipientMapper;

    public RecipientQueryService(RecipientRepository recipientRepository, RecipientMapper recipientMapper) {
        this.recipientRepository = recipientRepository;
        this.recipientMapper = recipientMapper;
    }

    /**
     * Return a {@link Page} of {@link RecipientDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<RecipientDTO> findByCriteria(RecipientCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Recipient> specification = createSpecification(criteria);
        return recipientRepository.findAll(specification, page).map(recipientMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(RecipientCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Recipient> specification = createSpecification(criteria);
        return recipientRepository.count(specification);
    }

    /**
     * Function to convert {@link RecipientCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Recipient> createSpecification(RecipientCriteria criteria) {
        Specification<Recipient> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Recipient_.organization, JoinType.LEFT);
                root.fetch(Recipient_.recipientGroup, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), Recipient_.id),
                    buildStringSpecification(criteria.getFirstName(), Recipient_.firstName),
                    buildStringSpecification(criteria.getLastName(), Recipient_.lastName),
                    buildStringSpecification(criteria.getEmail(), Recipient_.email),
                    buildRangeSpecification(criteria.getCreatedAt(), Recipient_.createdAt),
                    buildSpecification(criteria.getChannelsId(), root ->
                        root.join(Recipient_.channelses, JoinType.LEFT).get(RecipientChannel_.id)
                    ),
                    buildSpecification(criteria.getOrganizationId(), root ->
                        root.join(Recipient_.organization, JoinType.LEFT).get(Organization_.id)
                    ),
                    buildSpecification(criteria.getRecipientGroupId(), root ->
                        root.join(Recipient_.recipientGroup, JoinType.LEFT).get(RecipientGroup_.id)
                    )
                )
            );
        }
        return specification;
    }
}
