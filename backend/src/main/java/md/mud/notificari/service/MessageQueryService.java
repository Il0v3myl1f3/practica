package md.mud.notificari.service;

import jakarta.persistence.criteria.JoinType;
import md.mud.notificari.domain.*; // for static metamodels
import md.mud.notificari.domain.Message;
import md.mud.notificari.repository.MessageRepository;
import md.mud.notificari.service.criteria.MessageCriteria;
import md.mud.notificari.service.dto.MessageDTO;
import md.mud.notificari.service.mapper.MessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Message} entities in the database.
 * The main input is a {@link MessageCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link MessageDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class MessageQueryService extends QueryService<Message> {

    private static final Logger LOG = LoggerFactory.getLogger(MessageQueryService.class);

    private final MessageRepository messageRepository;

    private final MessageMapper messageMapper;

    public MessageQueryService(MessageRepository messageRepository, MessageMapper messageMapper) {
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
    }

    /**
     * Return a {@link Page} of {@link MessageDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<MessageDTO> findByCriteria(MessageCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Message> specification = createSpecification(criteria);
        return messageRepository.findAll(specification, page).map(messageMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(MessageCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Message> specification = createSpecification(criteria);
        return messageRepository.count(specification);
    }

    /**
     * Function to convert {@link MessageCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Message> createSpecification(MessageCriteria criteria) {
        Specification<Message> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Message_.organization, JoinType.LEFT);
                root.fetch(Message_.template, JoinType.LEFT);
                root.fetch(Message_.createdBy, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), Message_.id),
                    buildStringSpecification(criteria.getSubject(), Message_.subject),
                    buildSpecification(criteria.getStatus(), Message_.status),
                    buildRangeSpecification(criteria.getRecipientCount(), Message_.recipientCount),
                    buildRangeSpecification(criteria.getCreatedAt(), Message_.createdAt),
                    buildRangeSpecification(criteria.getSentAt(), Message_.sentAt),
                    buildSpecification(criteria.getChannelsId(), root ->
                        root.join(Message_.channelses, JoinType.LEFT).get(MessageChannel_.id)
                    ),
                    buildSpecification(criteria.getDeliveriesId(), root ->
                        root.join(Message_.deliverieses, JoinType.LEFT).get(MessageRecipient_.id)
                    ),
                    buildSpecification(criteria.getAttachmentsId(), root ->
                        root.join(Message_.attachmentses, JoinType.LEFT).get(MessageAttachment_.id)
                    ),
                    buildSpecification(criteria.getOrganizationId(), root ->
                        root.join(Message_.organization, JoinType.LEFT).get(Organization_.id)
                    ),
                    buildSpecification(criteria.getTemplateId(), root ->
                        root.join(Message_.template, JoinType.LEFT).get(MessageTemplate_.id)
                    ),
                    buildSpecification(criteria.getCreatedById(), root -> root.join(Message_.createdBy, JoinType.LEFT).get(User_.id))
                )
            );
        }
        return specification;
    }
}
