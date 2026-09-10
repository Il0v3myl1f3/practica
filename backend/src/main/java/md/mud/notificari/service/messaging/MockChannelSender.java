package md.mud.notificari.service.messaging;

import java.util.UUID;
import md.mud.notificari.domain.enumeration.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Telegram and WhatsApp have no API credentials yet, so delivery is logged and
 * reported as successful. Swap these for real clients without touching SendService:
 * both channels lose the subject line, which is prepended to the body instead.
 */
public abstract class MockChannelSender implements ChannelSender {

    private static final Logger LOG = LoggerFactory.getLogger(MockChannelSender.class);

    @Override
    public SendOutcome send(Delivery delivery) {
        String body = delivery.subject() == null || delivery.subject().isBlank()
            ? delivery.htmlBody()
            : delivery.subject() + "\n\n" + delivery.htmlBody();
        LOG.info("[{} MOCK] către {} ({}): {} caractere", channel(), delivery.recipientName(), delivery.address(), body.length());
        return SendOutcome.ok("mock-" + channel().name().toLowerCase() + "-" + UUID.randomUUID());
    }

    @Service
    public static class Telegram extends MockChannelSender {

        @Override
        public Channel channel() {
            return Channel.TELEGRAM;
        }
    }

    @Service
    public static class WhatsApp extends MockChannelSender {

        @Override
        public Channel channel() {
            return Channel.WHATSAPP;
        }
    }
}
