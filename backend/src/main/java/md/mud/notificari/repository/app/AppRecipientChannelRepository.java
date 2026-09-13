package md.mud.notificari.repository.app;

import java.util.List;
import md.mud.notificari.domain.RecipientChannel;
import md.mud.notificari.domain.enumeration.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppRecipientChannelRepository extends JpaRepository<RecipientChannel, Long> {
    List<RecipientChannel> findByRecipientId(Long recipientId);

    void deleteByRecipientId(Long recipientId);

    /**
     * Toate adresele unui canal din organizatie, cu destinatarul atasat - de aici
     * se vede ce chat ID e deja legat si la cine. Un singur query, nu unul per chat.
     */
    @Query(
        "select c from RecipientChannel c join fetch c.recipient r " +
        "where r.organization.id = :orgId and c.channel = :channel and r.archivedAt is null"
    )
    List<RecipientChannel> findAllForOrganizationAndChannel(Long orgId, Channel channel);
}
