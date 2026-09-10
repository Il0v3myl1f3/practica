package md.mud.notificari.repository;

import java.util.List;
import java.util.Optional;
import md.mud.notificari.domain.RecipientChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RecipientChannel entity.
 */
@Repository
public interface RecipientChannelRepository extends JpaRepository<RecipientChannel, Long> {
    default Optional<RecipientChannel> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<RecipientChannel> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<RecipientChannel> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select recipientChannel from RecipientChannel recipientChannel left join fetch recipientChannel.recipient",
        countQuery = "select count(recipientChannel) from RecipientChannel recipientChannel"
    )
    Page<RecipientChannel> findAllWithToOneRelationships(Pageable pageable);

    @Query("select recipientChannel from RecipientChannel recipientChannel left join fetch recipientChannel.recipient")
    List<RecipientChannel> findAllWithToOneRelationships();

    @Query(
        "select recipientChannel from RecipientChannel recipientChannel left join fetch recipientChannel.recipient where recipientChannel.id =:id"
    )
    Optional<RecipientChannel> findOneWithToOneRelationships(@Param("id") Long id);
}
