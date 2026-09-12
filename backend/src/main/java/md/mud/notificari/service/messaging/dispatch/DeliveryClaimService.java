package md.mud.notificari.service.messaging.dispatch;

import java.time.Instant;
import java.util.List;
import md.mud.notificari.domain.enumeration.Channel;
import md.mud.notificari.domain.enumeration.DeliveryStatus;
import md.mud.notificari.repository.app.AppMessageRecipientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rezerva livrari pentru firul curent, intr-o tranzactie foarte scurta. Nu face
 * niciodata I/O de retea - cand se intoarce, tranzactia s-a inchis deja.
 *
 * Bean separat de dispecer pentru ca {@code @Transactional} pe o metoda a
 * aceluiasi bean nu e interceptat: despartirea nu e de stil, e functionala.
 */
@Service
public class DeliveryClaimService {

    private static final List<DeliveryStatus> IN_FLIGHT = List.of(DeliveryStatus.PENDING, DeliveryStatus.SENDING);

    private final AppMessageRecipientRepository deliveries;
    private final RetryPolicy policy;

    public DeliveryClaimService(AppMessageRecipientRepository deliveries, RetryPolicy policy) {
        this.deliveries = deliveries;
        this.policy = policy;
    }

    @Transactional(readOnly = true)
    public List<Channel> channelsWithWork(Instant now) {
        return deliveries.channelsWithClaimable(IN_FLIGHT, now);
    }

    /** Calea asincrona: {@code for update skip locked}, ca doua instante sa nu ia acelasi rand. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> claimForDispatcher(Channel channel, int batchSize, Instant now) {
        return claim(deliveries.selectClaimable(channel.name(), now, batchSize), now);
    }

    /** Calea sincrona: un mesaj, un fir, nimic cu care sa concureze - deci fara lock (si H2 o poate rula). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> claimForMessage(Long messageId, Instant now) {
        return claim(deliveries.selectClaimableForMessage(messageId, IN_FLIGHT, now), now);
    }

    private List<Long> claim(List<Long> ids, Instant now) {
        if (ids.isEmpty()) {
            return List.of();
        }
        deliveries.markSending(ids, DeliveryStatus.SENDING, now, now.plus(policy.visibilityTimeout()));
        return ids;
    }
}
