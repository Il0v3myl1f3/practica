package md.mud.notificari.repository.app;

import java.util.List;
import java.util.Optional;
import md.mud.notificari.domain.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppRecipientRepository extends JpaRepository<Recipient, Long> {
    @Query(
        "select distinct r from Recipient r left join fetch r.channelses left join fetch r.recipientGroup " +
        "where r.organization.id = :orgId order by r.lastName asc, r.firstName asc"
    )
    List<Recipient> findAllForOrganization(Long orgId);

    Optional<Recipient> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Recipient> findByOrganizationIdAndEmailIgnoreCase(Long organizationId, String email);

    List<Recipient> findByOrganizationIdAndIdIn(Long organizationId, List<Long> ids);

    long countByOrganizationId(Long organizationId);
}
