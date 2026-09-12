package md.mud.notificari.service.messaging.dispatch;

import java.time.Instant;
import java.util.List;
import md.mud.notificari.config.ApplicationProperties;
import md.mud.notificari.domain.enumeration.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scurge coada de livrari, periodic.
 *
 * {@code fixedDelay} garanteaza ca un tick lent nu se suprapune cu el insusi,
 * deci pentru o singura instanta nu e nevoie de nimic in plus. Pentru mai multe
 * instante, claim-ul foloseste deja {@code for update skip locked}.
 *
 * Bean-ul exista doar in modul asincron: cand {@code application.messaging.async}
 * e false nu exista nimic de planificat.
 */
@Component
@ConditionalOnProperty(prefix = "application.messaging", name = "async", havingValue = "true", matchIfMissing = true)
public class DeliveryDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(DeliveryDispatcher.class);

    private final ApplicationProperties properties;
    private final DeliveryClaimService claims;
    private final DeliveryRunner runner;

    public DeliveryDispatcher(ApplicationProperties properties, DeliveryClaimService claims, DeliveryRunner runner) {
        this.properties = properties;
        this.claims = claims;
        this.runner = runner;
    }

    @Scheduled(fixedDelayString = "${application.messaging.dispatcher.poll-interval:5s}")
    public void tick() {
        if (!properties.getMessaging().isEnabled()) {
            // Trimiterile sunt oprite: randurile deja puse in coada raman si se
            // scurg cand se reporneste comutatorul.
            return;
        }
        Instant now = Instant.now();
        List<Channel> channels = claims.channelsWithWork(now);
        for (Channel channel : channels) {
            var settings = properties.getMessaging().settingsFor(channel);
            List<Long> claimed = claims.claimForDispatcher(channel, settings.getBatchSize(), now);
            if (claimed.isEmpty()) {
                continue;
            }
            int processed = runner.run(claimed, settings.getMinInterval());
            LOG.debug("{}: {} livrari procesate", channel, processed);
        }
    }
}
