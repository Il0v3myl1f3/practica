package md.mud.notificari.repository;

import java.util.List;
import java.util.Optional;
import md.mud.notificari.domain.MessageRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MessageRecipient entity.
 */
@Repository
public interface MessageRecipientRepository extends JpaRepository<MessageRecipient, Long> {
    default Optional<MessageRecipient> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<MessageRecipient> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<MessageRecipient> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select messageRecipient from MessageRecipient messageRecipient left join fetch messageRecipient.recipient left join fetch messageRecipient.message",
        countQuery = "select count(messageRecipient) from MessageRecipient messageRecipient"
    )
    Page<MessageRecipient> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select messageRecipient from MessageRecipient messageRecipient left join fetch messageRecipient.recipient left join fetch messageRecipient.message"
    )
    List<MessageRecipient> findAllWithToOneRelationships();

    @Query(
        "select messageRecipient from MessageRecipient messageRecipient left join fetch messageRecipient.recipient left join fetch messageRecipient.message where messageRecipient.id =:id"
    )
    Optional<MessageRecipient> findOneWithToOneRelationships(@Param("id") Long id);
}
