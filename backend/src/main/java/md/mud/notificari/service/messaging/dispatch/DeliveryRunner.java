package md.mud.notificari.service.messaging.dispatch;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import md.mud.notificari.service.messaging.ChannelSenderRegistry;
import md.mud.notificari.service.messaging.Delivery;
import md.mud.notificari.service.messaging.SendOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Bucla propriu-zisa de trimitere: incarca sarcina, cheama providerul, scrie
 * rezultatul. Deliberat <b>fara</b> {@code @Transactional} - apelul de retea nu
 * are voie sa se intample cu o conexiune JDBC in mana.
 *
 * E folosita si de dispecerul asincron, si de calea sincrona, ca modul sincron
 * sa exerseze exact codul care ruleaza in productie.
 */
@Component
public class DeliveryRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DeliveryRunner.class);

    private final ChannelSenderRegistry registry;
    private final DeliveryExecutor executor;
    private final MessageStatusRecalculator recalculator;

    public DeliveryRunner(ChannelSenderRegistry registry, DeliveryExecutor executor, MessageStatusRecalculator recalculator) {
        this.registry = registry;
        this.executor = executor;
        this.recalculator = recalculator;
    }

    /**
     * Trimite lotul rezervat si recalculeaza statusul mesajelor atinse.
     *
     * @return cate livrari au fost procesate
     */
    public int run(List<Long> deliveryIds, Duration minInterval) {
        if (deliveryIds.isEmpty()) {
            return 0;
        }
        Map<Long, List<Delivery.Attachment>> attachmentCache = new HashMap<>();
        Set<Long> touchedMessages = new LinkedHashSet<>();
        int processed = 0;
        for (Long deliveryId : deliveryIds) {
            processed += processOne(deliveryId, attachmentCache, touchedMessages) ? 1 : 0;
            pause(minInterval);
        }
        // Dupa fiecare lot, nu doar la final: asa niciun mesaj nu poate ramane
        // QUEUED pentru totdeauna daca un tick se opreste la mijloc.
        touchedMessages.forEach(recalculator::refresh);
        attachmentCache.clear();
        return processed;
    }

    private boolean processOne(Long deliveryId, Map<Long, List<Delivery.Attachment>> attachmentCache, Set<Long> touchedMessages) {
        DeliveryJob job = executor.load(deliveryId, attachmentCache).orElse(null);
        if (job == null) {
            // Destinatarul si-a pierdut adresa pe canal intre punerea in coada si acum.
            executor.markUnreachable(deliveryId);
            executor.messageIdOf(deliveryId).ifPresent(touchedMessages::add);
            return false;
        }
        touchedMessages.add(job.messageId());
        SendOutcome outcome;
        try {
            outcome = registry.forChannel(job.channel()).send(job.delivery());
        } catch (RuntimeException e) {
            // Un provider care arunca in loc sa intoarca un rezultat nu are voie
            // sa opreasca tot lotul.
            LOG.warn("Providerul {} a aruncat pentru livrarea {}: {}", job.channel(), deliveryId, e.getMessage());
            outcome = SendOutcome.retry(e.getMessage());
        }
        executor.complete(deliveryId, outcome);
        return true;
    }

    private static void pause(Duration minInterval) {
        if (minInterval == null || minInterval.isZero() || minInterval.isNegative()) {
            return;
        }
        try {
            Thread.sleep(minInterval.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
