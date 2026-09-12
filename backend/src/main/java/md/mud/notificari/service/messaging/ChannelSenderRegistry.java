package md.mud.notificari.service.messaging;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import md.mud.notificari.config.ApplicationProperties;
import md.mud.notificari.domain.enumeration.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Alege, la pornire, cate un {@link ChannelSender} pentru fiecare canal, dupa
 * {@code application.messaging.channels.<canal>.provider}.
 *
 * Toate implementarile raman bean-uri vii; registrul doar decide care ruleaza.
 * Asta face comutarea mock/real o chestiune de configuratie si pastreaza
 * greselile la pornire, nu in mijlocul unei trimiteri.
 */
@Component
public class ChannelSenderRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelSenderRegistry.class);

    private final Map<Channel, ChannelSender> active = new EnumMap<>(Channel.class);

    public ChannelSenderRegistry(List<ChannelSender> senders, ApplicationProperties properties) {
        Map<Channel, Map<String, ChannelSender>> byChannel = index(senders);
        for (Channel channel : Channel.values()) {
            Map<String, ChannelSender> available = byChannel.getOrDefault(channel, Map.of());
            String wanted = properties.getMessaging().settingsFor(channel).getProvider();
            ChannelSender chosen = available.get(wanted);
            if (chosen == null) {
                throw new IllegalStateException(
                    "application.messaging.channels." +
                    channel.name().toLowerCase(Locale.ROOT) +
                    ".provider=" +
                    wanted +
                    " nu exista. Disponibile: " +
                    available.keySet()
                );
            }
            chosen.validateConfiguration();
            active.put(channel, chosen);
        }
        LOG.info("Canale de trimitere: {}", describe());
    }

    public ChannelSender forChannel(Channel channel) {
        ChannelSender sender = active.get(channel);
        if (sender == null) {
            throw new IllegalStateException("Niciun sender configurat pentru canalul " + channel);
        }
        return sender;
    }

    /** Ce provider serveste fiecare canal - pentru loguri si diagnostic. */
    public Map<Channel, String> activeProviders() {
        Map<Channel, String> out = new EnumMap<>(Channel.class);
        active.forEach((channel, sender) -> out.put(channel, sender.providerId()));
        return out;
    }

    private static Map<Channel, Map<String, ChannelSender>> index(List<ChannelSender> senders) {
        Map<Channel, Map<String, ChannelSender>> byChannel = new EnumMap<>(Channel.class);
        for (ChannelSender sender : senders) {
            ChannelSender previous = byChannel
                .computeIfAbsent(sender.channel(), c -> new LinkedHashMap<>())
                .put(sender.providerId(), sender);
            if (previous != null) {
                throw new IllegalStateException(
                    "Doua implementari cu acelasi providerId '" +
                    sender.providerId() +
                    "' pentru canalul " +
                    sender.channel() +
                    ": " +
                    previous.getClass().getName() +
                    " si " +
                    sender.getClass().getName()
                );
            }
        }
        return byChannel;
    }

    private String describe() {
        StringBuilder sb = new StringBuilder();
        active.forEach((channel, sender) -> {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append(channel).append(" -> ").append(sender.providerId());
        });
        return sb.toString();
    }
}
