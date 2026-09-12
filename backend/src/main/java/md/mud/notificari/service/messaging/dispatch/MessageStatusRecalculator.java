package md.mud.notificari.service.messaging.dispatch;

import java.time.Instant;
import java.util.List;
import md.mud.notificari.domain.Message;
import md.mud.notificari.domain.enumeration.DeliveryStatus;
import md.mud.notificari.domain.enumeration.MessageStatus;
import md.mud.notificari.repository.app.AppMessageRecipientRepository;
import md.mud.notificari.repository.app.AppMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recalculeaza statusul mesajului din livrarile lui (regula 9 din app.jdl).
 *
 * Reproduce exact agregarea de dinaintea cozii - inclusiv cazul in care toate
 * livrarile sunt SKIPPED, care da SENT cu zero destinatari atinsi. Randurile de
 * istoric scrise inainte si dupa refactor trebuie sa insemne acelasi lucru.
 */
@Service
public class MessageStatusRecalculator {

    private static final List<DeliveryStatus> SUCCESSFUL = List.of(DeliveryStatus.SENT, DeliveryStatus.DELIVERED);

    private final AppMessageRepository messages;
    private final AppMessageRecipientRepository deliveries;

    public MessageStatusRecalculator(AppMessageRepository messages, AppMessageRecipientRepository deliveries) {
        this.messages = messages;
        this.deliveries = deliveries;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refresh(Long messageId) {
        List<DeliveryStatus> statuses = deliveries.statusesForMessage(messageId);
        if (statuses.stream().anyMatch(DeliveryStatus::isInFlight)) {
            // Inca se trimite: mesajul ramane QUEUED si nu-i atingem sentAt.
            return;
        }
        long delivered = statuses.stream().filter(DeliveryStatus::isSuccess).count();
        long failed = statuses.stream().filter(s -> s == DeliveryStatus.FAILED).count();
        MessageStatus status = failed == 0 ? MessageStatus.SENT : (delivered == 0 ? MessageStatus.FAILED : MessageStatus.PARTIAL);

        Message message = messages.findById(messageId).orElse(null);
        if (message == null) {
            return;
        }
        Instant sentAt = deliveries.lastSentAt(messageId);
        messages.save(
            message
                .status(status)
                .sentAt(sentAt == null ? Instant.now() : sentAt)
                .recipientCount((int) deliveries.countReachedRecipients(messageId, SUCCESSFUL))
        );
    }
}
