package md.mud.notificari.service.messaging;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import md.mud.notificari.domain.enumeration.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Providerul "mock": logheaza livrarea si o raporteaza reusita, fara sa iasa
 * nimic din aplicatie. Exista pentru fiecare canal, inclusiv EMAIL, si se alege
 * cu {@code application.messaging.channels.<canal>.provider: mock}.
 *
 * Telegram si WhatsApp nu au subiect (regula 8 din app.jdl), asa ca subiectul
 * devine prima linie a corpului - aceeasi conventie pe care va trebui sa o
 * pastreze si un client real.
 *
 * Bean-urile sunt declarate in {@link MessagingConfiguration}.
 */
public class MockChannelSender implements ChannelSender {

    private static final Logger LOG = LoggerFactory.getLogger(MockChannelSender.class);

    private final Channel channel;
    private final double failureRate;

    public MockChannelSender(Channel channel, double failureRate) {
        this.channel = channel;
        this.failureRate = failureRate;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public String providerId() {
        return "mock";
    }

    @Override
    public SendOutcome send(Delivery delivery) {
        if (failureRate > 0 && ThreadLocalRandom.current().nextDouble() < failureRate) {
            // Doar pentru a exersa retry-ul si backoff-ul fara retea.
            LOG.info("[{} MOCK] esec simulat catre {}", channel, delivery.address());
            return SendOutcome.retry("mock: esec simulat");
        }
        String body = delivery.subject() == null || delivery.subject().isBlank()
            ? delivery.htmlBody()
            : delivery.subject() + "\n\n" + delivery.htmlBody();
        LOG.info("[{} MOCK] către {} ({}): {} caractere", channel, delivery.recipientName(), delivery.address(), body.length());
        return SendOutcome.ok("mock-" + channel.name().toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID());
    }
}
