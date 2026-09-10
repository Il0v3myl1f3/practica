package md.mud.notificari.repository;

import java.util.List;
import java.util.Optional;
import md.mud.notificari.domain.MessageAttachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MessageAttachment entity.
 */
@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
    default Optional<MessageAttachment> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<MessageAttachment> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<MessageAttachment> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select messageAttachment from MessageAttachment messageAttachment left join fetch messageAttachment.message",
        countQuery = "select count(messageAttachment) from MessageAttachment messageAttachment"
    )
    Page<MessageAttachment> findAllWithToOneRelationships(Pageable pageable);

    @Query("select messageAttachment from MessageAttachment messageAttachment left join fetch messageAttachment.message")
    List<MessageAttachment> findAllWithToOneRelationships();

    @Query(
        "select messageAttachment from MessageAttachment messageAttachment left join fetch messageAttachment.message where messageAttachment.id =:id"
    )
    Optional<MessageAttachment> findOneWithToOneRelationships(@Param("id") Long id);
}
