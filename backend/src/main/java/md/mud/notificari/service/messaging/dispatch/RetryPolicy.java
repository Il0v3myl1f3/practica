package md.mud.notificari.service.messaging.dispatch;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import md.mud.notificari.config.ApplicationProperties;
import org.springframework.stereotype.Component;

/**
 * Cat se asteapta intre incercari si cand se renunta.
 *
 * Cu implicitele (30s, x3, plafon 1h, 5 incercari): ~30s, ~90s, ~4.5min,
 * ~13.5min, apoi FAILED - o fereastra de vreo 19 minute, cat sa treaca o pana
 * de retea fara ca un rand sa stea galben ore intregi.
 */
@Component
public class RetryPolicy {

    private final ApplicationProperties.Dispatcher config;

    public RetryPolicy(ApplicationProperties properties) {
        this.config = properties.getMessaging().getDispatcher();
    }

    /**
     * @param attempt cate incercari s-au facut deja (1 dupa prima)
     */
    public Duration delayFor(int attempt) {
        double raw = config.getBackoffInitial().toMillis() * Math.pow(config.getBackoffMultiplier(), Math.max(0, attempt - 1));
        double capped = Math.min(raw, config.getBackoffMax().toMillis());
        // Jitter: cand pica reteaua, tot lotul esueaza in aceeasi secunda. Fara
        // el, tot lotul ar si reincerca in aceeasi secunda.
        double jitter = config.getBackoffJitter();
        double spread = jitter <= 0 ? 1 : 1 + jitter * (ThreadLocalRandom.current().nextDouble() * 2 - 1);
        return Duration.ofMillis(Math.max(1, (long) (capped * spread)));
    }

    /**
     * Ca {@link #delayFor(int)}, dar respecta pauza ceruta de provider cand e mai
     * lunga decat a noastra. Mai scurta n-o luam: un provider nu are de unde sti
     * cat ne permitem sa asteptam, iar backoff-ul nostru e deja prudent.
     */
    public Duration delayFor(int attempt, Duration retryAfter) {
        Duration ours = delayFor(attempt);
        return retryAfter == null || retryAfter.compareTo(ours) <= 0 ? ours : retryAfter;
    }

    public boolean exhausted(int attempt) {
        return attempt >= config.getMaxAttempts();
    }

    public Duration visibilityTimeout() {
        return config.getVisibilityTimeout();
    }
}
