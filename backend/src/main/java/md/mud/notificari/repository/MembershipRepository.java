package md.mud.notificari.repository;

import java.util.List;
import java.util.Optional;
import md.mud.notificari.domain.Membership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Membership entity.
 */
@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    @Query("select membership from Membership membership where membership.user.login = ?#{authentication.name}")
    List<Membership> findByUserIsCurrentUser();

    default Optional<Membership> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Membership> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Membership> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select membership from Membership membership left join fetch membership.user left join fetch membership.organization",
        countQuery = "select count(membership) from Membership membership"
    )
    Page<Membership> findAllWithToOneRelationships(Pageable pageable);

    @Query("select membership from Membership membership left join fetch membership.user left join fetch membership.organization")
    List<Membership> findAllWithToOneRelationships();

    @Query(
        "select membership from Membership membership left join fetch membership.user left join fetch membership.organization where membership.id =:id"
    )
    Optional<Membership> findOneWithToOneRelationships(@Param("id") Long id);
}
