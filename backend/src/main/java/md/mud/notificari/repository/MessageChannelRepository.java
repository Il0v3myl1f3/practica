package md.mud.notificari.repository;

import java.util.List;
import java.util.Optional;
import md.mud.notificari.domain.MessageChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MessageChannel entity.
 */
@Repository
public interface MessageChannelRepository extends JpaRepository<MessageChannel, Long> {
    default Optional<MessageChannel> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<MessageChannel> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<MessageChannel> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select messageChannel from MessageChannel messageChannel left join fetch messageChannel.message",
        countQuery = "select count(messageChannel) from MessageChannel messageChannel"
    )
    Page<MessageChannel> findAllWithToOneRelationships(Pageable pageable);

    @Query("select messageChannel from MessageChannel messageChannel left join fetch messageChannel.message")
    List<MessageChannel> findAllWithToOneRelationships();

    @Query("select messageChannel from MessageChannel messageChannel left join fetch messageChannel.message where messageChannel.id =:id")
    Optional<MessageChannel> findOneWithToOneRelationships(@Param("id") Long id);
}
