package md.mud.notificari.repository;

import java.util.List;
import java.util.Optional;
import md.mud.notificari.domain.RecipientGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RecipientGroup entity.
 */
@Repository
public interface RecipientGroupRepository extends JpaRepository<RecipientGroup, Long> {
    default Optional<RecipientGroup> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<RecipientGroup> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<RecipientGroup> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select recipientGroup from RecipientGroup recipientGroup left join fetch recipientGroup.organization",
        countQuery = "select count(recipientGroup) from RecipientGroup recipientGroup"
    )
    Page<RecipientGroup> findAllWithToOneRelationships(Pageable pageable);

    @Query("select recipientGroup from RecipientGroup recipientGroup left join fetch recipientGroup.organization")
    List<RecipientGroup> findAllWithToOneRelationships();

    @Query(
        "select recipientGroup from RecipientGroup recipientGroup left join fetch recipientGroup.organization where recipientGroup.id =:id"
    )
    Optional<RecipientGroup> findOneWithToOneRelationships(@Param("id") Long id);
}
