package md.mud.notificari.repository.app;

import java.util.List;
import java.util.Optional;
import md.mud.notificari.domain.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppMessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {
    List<MessageTemplate> findByOrganizationIdOrderByNameAsc(Long organizationId);

    Optional<MessageTemplate> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(Long organizationId, String name, Long id);

    long countByOrganizationId(Long organizationId);
}
