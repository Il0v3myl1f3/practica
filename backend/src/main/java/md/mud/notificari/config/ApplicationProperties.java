package md.mud.notificari.config;

import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import md.mud.notificari.domain.enumeration.Channel;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Notificari Mud.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 * <p>
 * Atentie: {@code ignoreUnknownFields = false}. Orice cheie noua sub
 * {@code application.*} trebuie sa aiba camp aici, altfel aplicatia nu porneste.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();

    private final Messaging messaging = new Messaging();

    // jhipster-needle-application-properties-property

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public Messaging getMessaging() {
        return messaging;
    }

    // jhipster-needle-application-properties-property-getter

    public static class Liquibase {

        private Boolean asyncStart = true;

        public Boolean getAsyncStart() {
            return asyncStart;
        }

        public void setAsyncStart(Boolean asyncStart) {
            this.asyncStart = asyncStart;
        }
    }

    /**
     * Trimiterea mesajelor. Doua butoane, cu intelesuri diferite:
     * <ul>
     *   <li>{@code enabled: false} - oprit. Trimiterea e refuzata, dispecerul sta.</li>
     *   <li>{@code channels.<canal>.provider: mock} - pornit, dar simulat: fluxul
     *       merge cap-coada si istoricul se populeaza, dar nimic nu pleaca.</li>
     * </ul>
     */
    public static class Messaging {

        private boolean enabled = true;

        /** false = se trimite inline, in cererea HTTP, ca inainte de coada. */
        private boolean async = true;

        private final Map<Channel, ChannelSettings> channels = new EnumMap<>(Channel.class);

        private final Dispatcher dispatcher = new Dispatcher();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAsync() {
            return async;
        }

        public void setAsync(boolean async) {
            this.async = async;
        }

        public Map<Channel, ChannelSettings> getChannels() {
            return channels;
        }

        public Dispatcher getDispatcher() {
            return dispatcher;
        }

        /** Un canal nementionat in yaml ramane pe mock, ca sa nu plece nimic din greseala. */
        public ChannelSettings settingsFor(Channel channel) {
            return channels.computeIfAbsent(channel, c -> new ChannelSettings());
        }
    }

    public static class ChannelSettings {

        /** Id-ul providerului, adica ChannelSender.providerId(): "mock", "smtp", ... */
        private String provider = "mock";

        /** Cate livrari ia dispecerul pe canal, la fiecare tick. */
        private int batchSize = 25;

        /** Pauza intre doua trimiteri consecutive pe acelasi canal. */
        private Duration minInterval = Duration.ZERO;

        /** Doar pentru providerul mock: fractiunea de trimiteri care esueaza, ca sa se poata exersa retry-ul. */
        private double mockFailureRate = 0d;

        /**
         * Setari specifice providerului (token, url, numar de telefon...). E aici
         * ca un client real de Telegram sau Discord sa se configureze fara sa mai
         * fie nevoie de un camp nou in clasa asta.
         */
        private final Map<String, String> options = new LinkedHashMap<>();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public Duration getMinInterval() {
            return minInterval;
        }

        public void setMinInterval(Duration minInterval) {
            this.minInterval = minInterval;
        }

        public double getMockFailureRate() {
            return mockFailureRate;
        }

        public void setMockFailureRate(double mockFailureRate) {
            this.mockFailureRate = mockFailureRate;
        }

        public Map<String, String> getOptions() {
            return options;
        }
    }

    public static class Dispatcher {

        private Duration pollInterval = Duration.ofSeconds(5);

        /** Cat timp o livrare poate sta SENDING inainte sa fie considerata abandonata si reluata. */
        private Duration visibilityTimeout = Duration.ofMinutes(5);

        private int maxAttempts = 5;

        private Duration backoffInitial = Duration.ofSeconds(30);

        private double backoffMultiplier = 3d;

        private Duration backoffMax = Duration.ofHours(1);

        /** +/- fractiunea asta peste intarziere, ca un batch picat sa nu reincerce tot in aceeasi secunda. */
        private double backoffJitter = 0.2d;

        public Duration getPollInterval() {
            return pollInterval;
        }

        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }

        public Duration getVisibilityTimeout() {
            return visibilityTimeout;
        }

        public void setVisibilityTimeout(Duration visibilityTimeout) {
            this.visibilityTimeout = visibilityTimeout;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBackoffInitial() {
            return backoffInitial;
        }

        public void setBackoffInitial(Duration backoffInitial) {
            this.backoffInitial = backoffInitial;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public void setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }

        public Duration getBackoffMax() {
            return backoffMax;
        }

        public void setBackoffMax(Duration backoffMax) {
            this.backoffMax = backoffMax;
        }

        public double getBackoffJitter() {
            return backoffJitter;
        }

        public void setBackoffJitter(double backoffJitter) {
            this.backoffJitter = backoffJitter;
        }
    }

    // jhipster-needle-application-properties-property-class
}
