package md.mud.notificari.repository.app;

import java.util.List;
import java.util.Optional;
import md.mud.notificari.domain.Message;
import md.mud.notificari.domain.enumeration.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppMessageRepository extends JpaRepository<Message, Long> {
    @Query(
        "select distinct m from Message m left join fetch m.channelses " +
        "where m.organization.id = :orgId and m.status = :status order by m.createdAt desc"
    )
    List<Message> findAllForOrganization(Long orgId, MessageStatus status);

    @Query(
        "select distinct m from Message m left join fetch m.channelses " +
        "where m.organization.id = :orgId and m.status <> :status order by m.createdAt desc"
    )
    List<Message> findAllForOrganizationExcept(Long orgId, MessageStatus status);

    Optional<Message> findByIdAndOrganizationId(Long id, Long organizationId);

    long countByOrganizationIdAndStatus(Long organizationId, MessageStatus status);

    long countByOrganizationIdAndStatusNot(Long organizationId, MessageStatus status);
}
