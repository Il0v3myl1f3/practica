package md.mud.notificari.service.messaging;

import md.mud.notificari.config.ApplicationProperties;
import md.mud.notificari.domain.enumeration.Channel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean-urile mock, cate unul per canal - inclusiv EMAIL, care pana acum isi
 * purta mock-ul ascuns in sender-ul real.
 *
 * Sunt declarate explicit, nu descoperite prin scanare, ca sa se vada dintr-o
 * privire ce providere exista si ca sa poata fi inlocuite intr-un test.
 */
@Configuration
public class MessagingConfiguration {

    @Bean
    public ChannelSender mockEmailSender(ApplicationProperties properties) {
        return mock(Channel.EMAIL, properties);
    }

    @Bean
    public ChannelSender mockTelegramSender(ApplicationProperties properties) {
        return mock(Channel.TELEGRAM, properties);
    }

    @Bean
    public ChannelSender mockDiscordSender(ApplicationProperties properties) {
        return mock(Channel.DISCORD, properties);
    }

    private static ChannelSender mock(Channel channel, ApplicationProperties properties) {
        return new MockChannelSender(channel, properties.getMessaging().settingsFor(channel).getMockFailureRate());
    }
}
