package md.mud.notificari.service.criteria;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link md.mud.notificari.domain.Recipient} entity. This class is used
 * in {@link md.mud.notificari.web.rest.RecipientResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /recipients?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RecipientCriteria implements Serializable, Criteria {

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter firstName;

    private StringFilter lastName;

    private StringFilter email;

    private InstantFilter createdAt;

    private LongFilter channelsId;

    private LongFilter organizationId;

    private LongFilter recipientGroupId;

    private Boolean distinct;

    public RecipientCriteria() {}

    public RecipientCriteria(RecipientCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.firstName = other.optionalFirstName().map(StringFilter::copy).orElse(null);
        this.lastName = other.optionalLastName().map(StringFilter::copy).orElse(null);
        this.email = other.optionalEmail().map(StringFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.channelsId = other.optionalChannelsId().map(LongFilter::copy).orElse(null);
        this.organizationId = other.optionalOrganizationId().map(LongFilter::copy).orElse(null);
        this.recipientGroupId = other.optionalRecipientGroupId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public RecipientCriteria copy() {
        return new RecipientCriteria(this);
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

    public StringFilter getFirstName() {
        return firstName;
    }

    public Optional<StringFilter> optionalFirstName() {
        return Optional.ofNullable(firstName);
    }

    public StringFilter firstName() {
        if (firstName == null) {
            setFirstName(new StringFilter());
        }
        return firstName;
    }

    public void setFirstName(StringFilter firstName) {
        this.firstName = firstName;
    }

    public StringFilter getLastName() {
        return lastName;
    }

    public Optional<StringFilter> optionalLastName() {
        return Optional.ofNullable(lastName);
    }

    public StringFilter lastName() {
        if (lastName == null) {
            setLastName(new StringFilter());
        }
        return lastName;
    }

    public void setLastName(StringFilter lastName) {
        this.lastName = lastName;
    }

    public StringFilter getEmail() {
        return email;
    }

    public Optional<StringFilter> optionalEmail() {
        return Optional.ofNullable(email);
    }

    public StringFilter email() {
        if (email == null) {
            setEmail(new StringFilter());
        }
        return email;
    }

    public void setEmail(StringFilter email) {
        this.email = email;
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

    public LongFilter getRecipientGroupId() {
        return recipientGroupId;
    }

    public Optional<LongFilter> optionalRecipientGroupId() {
        return Optional.ofNullable(recipientGroupId);
    }

    public LongFilter recipientGroupId() {
        if (recipientGroupId == null) {
            setRecipientGroupId(new LongFilter());
        }
        return recipientGroupId;
    }

    public void setRecipientGroupId(LongFilter recipientGroupId) {
        this.recipientGroupId = recipientGroupId;
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
        final RecipientCriteria that = (RecipientCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(firstName, that.firstName) &&
            Objects.equals(lastName, that.lastName) &&
            Objects.equals(email, that.email) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(channelsId, that.channelsId) &&
            Objects.equals(organizationId, that.organizationId) &&
            Objects.equals(recipientGroupId, that.recipientGroupId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, email, createdAt, channelsId, organizationId, recipientGroupId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RecipientCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalFirstName().map(f -> "firstName=" + f + ", ").orElse("") +
            optionalLastName().map(f -> "lastName=" + f + ", ").orElse("") +
            optionalEmail().map(f -> "email=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalChannelsId().map(f -> "channelsId=" + f + ", ").orElse("") +
            optionalOrganizationId().map(f -> "organizationId=" + f + ", ").orElse("") +
            optionalRecipientGroupId().map(f -> "recipientGroupId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
