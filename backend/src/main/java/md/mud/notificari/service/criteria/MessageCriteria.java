package md.mud.notificari.service.criteria;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import md.mud.notificari.domain.enumeration.MessageStatus;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link md.mud.notificari.domain.Message} entity. This class is used
 * in {@link md.mud.notificari.web.rest.MessageResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /messages?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageCriteria implements Serializable, Criteria {

    /**
     * Class for filtering MessageStatus
     */
    public static class MessageStatusFilter extends Filter<MessageStatus> {

        public MessageStatusFilter() {}

        public MessageStatusFilter(MessageStatusFilter filter) {
            super(filter);
        }

        @Override
        public MessageStatusFilter copy() {
            return new MessageStatusFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter subject;

    private MessageStatusFilter status;

    private IntegerFilter recipientCount;

    private InstantFilter createdAt;

    private InstantFilter sentAt;

    private LongFilter channelsId;

    private LongFilter deliveriesId;

    private LongFilter attachmentsId;

    private LongFilter organizationId;

    private LongFilter templateId;

    private LongFilter createdById;

    private Boolean distinct;

    public MessageCriteria() {}

    public MessageCriteria(MessageCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.subject = other.optionalSubject().map(StringFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(MessageStatusFilter::copy).orElse(null);
        this.recipientCount = other.optionalRecipientCount().map(IntegerFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.sentAt = other.optionalSentAt().map(InstantFilter::copy).orElse(null);
        this.channelsId = other.optionalChannelsId().map(LongFilter::copy).orElse(null);
        this.deliveriesId = other.optionalDeliveriesId().map(LongFilter::copy).orElse(null);
        this.attachmentsId = other.optionalAttachmentsId().map(LongFilter::copy).orElse(null);
        this.organizationId = other.optionalOrganizationId().map(LongFilter::copy).orElse(null);
        this.templateId = other.optionalTemplateId().map(LongFilter::copy).orElse(null);
        this.createdById = other.optionalCreatedById().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public MessageCriteria copy() {
        return new MessageCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getSubject() {
        return subject;
    }

    public Optional<StringFilter> optionalSubject() {
        return Optional.ofNullable(subject);
    }

    public StringFilter subject() {
        if (subject == null) {
            setSubject(new StringFilter());
        }
        return subject;
    }

    public void setSubject(StringFilter subject) {
        this.subject = subject;
    }

    public MessageStatusFilter getStatus() {
        return status;
    }

    public Optional<MessageStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public MessageStatusFilter status() {
        if (status == null) {
            setStatus(new MessageStatusFilter());
        }
        return status;
    }

    public void setStatus(MessageStatusFilter status) {
        this.status = status;
    }

    public IntegerFilter getRecipientCount() {
        return recipientCount;
    }

    public Optional<IntegerFilter> optionalRecipientCount() {
        return Optional.ofNullable(recipientCount);
    }

    public IntegerFilter recipientCount() {
        if (recipientCount == null) {
            setRecipientCount(new IntegerFilter());
        }
        return recipientCount;
    }

    public void setRecipientCount(IntegerFilter recipientCount) {
        this.recipientCount = recipientCount;
    }

    public InstantFilter getCreatedAt() {
        return createdAt;
    }

    public Optional<InstantFilter> optionalCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public InstantFilter createdAt() {
        if (createdAt == null) {
            setCreatedAt(new InstantFilter());
        }
        return createdAt;
    }

    public void setCreatedAt(InstantFilter createdAt) {
        this.createdAt = createdAt;
    }

    public InstantFilter getSentAt() {
        return sentAt;
    }

    public Optional<InstantFilter> optionalSentAt() {
        return Optional.ofNullable(sentAt);
    }

    public InstantFilter sentAt() {
        if (sentAt == null) {
            setSentAt(new InstantFilter());
        }
        return sentAt;
    }

    public void setSentAt(InstantFilter sentAt) {
        this.sentAt = sentAt;
    }

    public LongFilter getChannelsId() {
        return channelsId;
    }

    public Optional<LongFilter> optionalChannelsId() {
        return Optional.ofNullable(channelsId);
    }

    public LongFilter channelsId() {
        if (channelsId == null) {
            setChannelsId(new LongFilter());
        }
        return channelsId;
    }

    public void setChannelsId(LongFilter channelsId) {
        this.channelsId = channelsId;
    }

    public LongFilter getDeliveriesId() {
        return deliveriesId;
    }

    public Optional<LongFilter> optionalDeliveriesId() {
        return Optional.ofNullable(deliveriesId);
    }

    public LongFilter deliveriesId() {
        if (deliveriesId == null) {
            setDeliveriesId(new LongFilter());
        }
        return deliveriesId;
    }

    public void setDeliveriesId(LongFilter deliveriesId) {
        this.deliveriesId = deliveriesId;
    }

    public LongFilter getAttachmentsId() {
        return attachmentsId;
    }

    public Optional<LongFilter> optionalAttachmentsId() {
        return Optional.ofNullable(attachmentsId);
    }

    public LongFilter attachmentsId() {
        if (attachmentsId == null) {
            setAttachmentsId(new LongFilter());
        }
        return attachmentsId;
    }

    public void setAttachmentsId(LongFilter attachmentsId) {
        this.attachmentsId = attachmentsId;
    }

    public LongFilter getOrganizationId() {
        return organizationId;
    }

    public Optional<LongFilter> optionalOrganizationId() {
        return Optional.ofNullable(organizationId);
    }

    public LongFilter organizationId() {
        if (organizationId == null) {
            setOrganizationId(new LongFilter());
        }
        return organizationId;
    }

    public void setOrganizationId(LongFilter organizationId) {
        this.organizationId = organizationId;
    }

    public LongFilter getTemplateId() {
        return templateId;
    }

    public Optional<LongFilter> optionalTemplateId() {
        return Optional.ofNullable(templateId);
    }

    public LongFilter templateId() {
        if (templateId == null) {
            setTemplateId(new LongFilter());
        }
        return templateId;
    }

    public void setTemplateId(LongFilter templateId) {
        this.templateId = templateId;
    }

    public LongFilter getCreatedById() {
        return createdById;
    }

    public Optional<LongFilter> optionalCreatedById() {
        return Optional.ofNullable(createdById);
    }

    public LongFilter createdById() {
        if (createdById == null) {
            setCreatedById(new LongFilter());
        }
        return createdById;
    }

    public void setCreatedById(LongFilter createdById) {
        this.createdById = createdById;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MessageCriteria that = (MessageCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(subject, that.subject) &&
            Objects.equals(status, that.status) &&
            Objects.equals(recipientCount, that.recipientCount) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(sentAt, that.sentAt) &&
            Objects.equals(channelsId, that.channelsId) &&
            Objects.equals(deliveriesId, that.deliveriesId) &&
            Objects.equals(attachmentsId, that.attachmentsId) &&
            Objects.equals(organizationId, that.organizationId) &&
            Objects.equals(templateId, that.templateId) &&
            Objects.equals(createdById, that.createdById) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            subject,
            status,
            recipientCount,
            createdAt,
            sentAt,
            channelsId,
            deliveriesId,
            attachmentsId,
            organizationId,
            templateId,
            createdById,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalSubject().map(f -> "subject=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalRecipientCount().map(f -> "recipientCount=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalSentAt().map(f -> "sentAt=" + f + ", ").orElse("") +
            optionalChannelsId().map(f -> "channelsId=" + f + ", ").orElse("") +
            optionalDeliveriesId().map(f -> "deliveriesId=" + f + ", ").orElse("") +
            optionalAttachmentsId().map(f -> "attachmentsId=" + f + ", ").orElse("") +
            optionalOrganizationId().map(f -> "organizationId=" + f + ", ").orElse("") +
            optionalTemplateId().map(f -> "templateId=" + f + ", ").orElse("") +
            optionalCreatedById().map(f -> "createdById=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
