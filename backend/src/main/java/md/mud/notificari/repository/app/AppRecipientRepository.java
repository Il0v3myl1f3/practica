package md.mud.notificari.repository.app;

import java.util.List;
import java.util.Optional;
import md.mud.notificari.domain.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Destinatarii arhivati ({@code archivedAt} nenul) sunt scosi din liste, din
 * numaratori si din tintele trimiterii; raman doar ca sa nu rupa istoricul
 * livrarilor. Singura metoda care ii mai vede e {@code findByIdAndOrganizationId},
 * folosita de editare si stergere.
 */
public interface AppRecipientRepository extends JpaRepository<Recipient, Long> {
    @Query(
        "select distinct r from Recipient r left join fetch r.channelses left join fetch r.recipientGroup " +
        "where r.organization.id = :orgId and r.archivedAt is null order by r.lastName asc, r.firstName asc"
    )
    List<Recipient> findAllForOrganization(Long orgId);

    Optional<Recipient> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Recipient> findByOrganizationIdAndEmailIgnoreCaseAndArchivedAtIsNull(Long organizationId, String email);

    List<Recipient> findByOrganizationIdAndArchivedAtIsNullAndIdIn(Long organizationId, List<Long> ids);

    long countByOrganizationIdAndArchivedAtIsNull(Long organizationId);

    /** Include si arhivatii: tot tin FK-ul spre grup, l-ar rupe daca grupul e sters sub ei. */
    long countByRecipientGroupId(Long recipientGroupId);
}
