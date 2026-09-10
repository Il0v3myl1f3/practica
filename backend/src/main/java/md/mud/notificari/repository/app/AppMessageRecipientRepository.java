package md.mud.notificari.repository.app;

import java.util.List;
import md.mud.notificari.domain.MessageRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppMessageRecipientRepository extends JpaRepository<MessageRecipient, Long> {
    @Query("select distinct mr from MessageRecipient mr left join fetch mr.recipient where mr.message.id = :messageId")
    List<MessageRecipient> findAllForMessage(Long messageId);
}
