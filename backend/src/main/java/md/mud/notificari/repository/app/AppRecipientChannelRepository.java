package md.mud.notificari.repository.app;

import java.util.List;
import md.mud.notificari.domain.RecipientChannel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRecipientChannelRepository extends JpaRepository<RecipientChannel, Long> {
    List<RecipientChannel> findByRecipientId(Long recipientId);

    void deleteByRecipientId(Long recipientId);
}
