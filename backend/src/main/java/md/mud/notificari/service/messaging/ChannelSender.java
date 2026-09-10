package md.mud.notificari.service.messaging;

import md.mud.notificari.domain.enumeration.Channel;

/**
 * One implementation per delivery channel. Only email talks to a real provider
 * for now; Telegram and WhatsApp are mocked until we have API credentials.
 */
public interface ChannelSender {
    Channel channel();

    SendOutcome send(Delivery delivery);
}
