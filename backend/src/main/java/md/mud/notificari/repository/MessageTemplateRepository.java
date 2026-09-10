package md.mud.notificari.repository;

import java.util.List;
import java.util.Optional;
import md.mud.notificari.domain.MessageTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MessageTemplate entity.
 */
@Repository
public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {
    default Optional<MessageTemplate> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<MessageTemplate> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<MessageTemplate> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select messageTemplate from MessageTemplate messageTemplate left join fetch messageTemplate.organization",
        countQuery = "select count(messageTemplate) from MessageTemplate messageTemplate"
    )
    Page<MessageTemplate> findAllWithToOneRelationships(Pageable pageable);

    @Query("select messageTemplate from MessageTemplate messageTemplate left join fetch messageTemplate.organization")
    List<MessageTemplate> findAllWithToOneRelationships();

    @Query(
        "select messageTemplate from MessageTemplate messageTemplate left join fetch messageTemplate.organization where messageTemplate.id =:id"
    )
    Optional<MessageTemplate> findOneWithToOneRelationships(@Param("id") Long id);
}
