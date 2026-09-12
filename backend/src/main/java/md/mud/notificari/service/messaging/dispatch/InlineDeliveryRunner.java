package md.mud.notificari.service.messaging.dispatch;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import md.mud.notificari.config.ApplicationProperties;
import org.springframework.stereotype.Component;

/**
 * Calea sincrona ({@code application.messaging.async: false}): trimite livrarile
 * unui mesaj pe loc, in cererea HTTP, exact prin aceleasi clase ca dispecerul.
 *
 * Nu doarme prin backoff: o livrare intoarsa in PENDING cu {@code nextAttemptAt}
 * in viitor ramane acolo. In profilul de test se pune {@code max-attempts: 1},
 * ca un esec sa fie definitiv si testul sa nu astepte.
 */
@Component
public class InlineDeliveryRunner {

    /** Plasa de siguranta: chiar daca ceva reintroduce randuri, nu invartim la nesfarsit. */
    private static final int MAX_ROUNDS = 50;

    private final ApplicationProperties properties;
    private final DeliveryClaimService claims;
    private final DeliveryRunner runner;

    public InlineDeliveryRunner(ApplicationProperties properties, DeliveryClaimService claims, DeliveryRunner runner) {
        this.properties = properties;
        this.claims = claims;
        this.runner = runner;
    }

    public void drain(Long messageId) {
        if (!properties.getMessaging().isEnabled()) {
            return;
        }
        for (int round = 0; round < MAX_ROUNDS; round++) {
            List<Long> claimed = claims.claimForMessage(messageId, Instant.now());
            if (claimed.isEmpty()) {
                return;
            }
            runner.run(claimed, Duration.ZERO);
        }
    }
}
