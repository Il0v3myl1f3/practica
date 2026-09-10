package md.mud.notificari.repository.app;

import java.util.Optional;
import md.mud.notificari.domain.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppMembershipRepository extends JpaRepository<Membership, Long> {
    @Query("select m from Membership m join fetch m.organization where m.user.id = :userId order by m.id asc limit 1")
    Optional<Membership> findFirstForUser(Long userId);
}
