package md.mud.notificari.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * fullName NU se stocheaza: este derivat din firstName + \" \" + lastName.
 */
@Entity
@Table(name = "recipient")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Recipient implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 60)
    @Column(name = "first_name", length = 60, nullable = false)
    private String firstName;

    @NotNull
    @Size(max = 60)
    @Column(name = "last_name", length = 60, nullable = false)
    private String lastName;

    @NotNull
    @Size(max = 254)
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    @Column(name = "email", length = 254, nullable = false)
    private String email;

    @Column(name = "created_at")
    private Instant createdAt;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "recipient")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "recipient" }, allowSetters = true)
    private Set<RecipientChannel> channelses = new HashSet<>();

    @ManyToOne(optional = false)
    @NotNull
    private Organization organization;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "organization" }, allowSetters = true)
    private RecipientGroup recipientGroup;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Recipient id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public Recipient firstName(String firstName) {
        this.setFirstName(firstName);
        return this;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public Recipient lastName(String lastName) {
        this.setLastName(lastName);
        return this;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return this.email;
    }

    public Recipient email(String email) {
        this.setEmail(email);
        return this;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Recipient createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Set<RecipientChannel> getChannelses() {
        return this.channelses;
    }

    public void setChannelses(Set<RecipientChannel> recipientChannels) {
        if (this.channelses != null) {
            this.channelses.forEach(i -> i.setRecipient(null));
        }
        if (recipientChannels != null) {
            recipientChannels.forEach(i -> i.setRecipient(this));
        }
        this.channelses = recipientChannels;
    }

    public Recipient channelses(Set<RecipientChannel> recipientChannels) {
        this.setChannelses(recipientChannels);
        return this;
    }

    public Recipient addChannels(RecipientChannel recipientChannel) {
        this.channelses.add(recipientChannel);
        recipientChannel.setRecipient(this);
        return this;
    }

    public Recipient removeChannels(RecipientChannel recipientChannel) {
        this.channelses.remove(recipientChannel);
        recipientChannel.setRecipient(null);
        return this;
    }

    public Organization getOrganization() {
        return this.organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Recipient organization(Organization organization) {
        this.setOrganization(organization);
        return this;
    }

    public RecipientGroup getRecipientGroup() {
        return this.recipientGroup;
    }

    public void setRecipientGroup(RecipientGroup recipientGroup) {
        this.recipientGroup = recipientGroup;
    }

    public Recipient recipientGroup(RecipientGroup recipientGroup) {
        this.setRecipientGroup(recipientGroup);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Recipient)) {
            return false;
        }
        return getId() != null && getId().equals(((Recipient) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Recipient{" +
            "id=" + getId() +
            ", firstName='" + getFirstName() + "'" +
            ", lastName='" + getLastName() + "'" +
            ", email='" + getEmail() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
