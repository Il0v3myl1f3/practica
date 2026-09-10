package md.mud.notificari.repository.app;

import java.util.List;
import java.util.Optional;
import md.mud.notificari.domain.RecipientGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRecipientGroupRepository extends JpaRepository<RecipientGroup, Long> {
    List<RecipientGroup> findByOrganizationIdOrderByNameAsc(Long organizationId);

    Optional<RecipientGroup> findByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);

    long countByOrganizationId(Long organizationId);
}
