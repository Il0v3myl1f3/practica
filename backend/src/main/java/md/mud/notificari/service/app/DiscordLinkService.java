package md.mud.notificari.service.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import md.mud.notificari.domain.Recipient;
import md.mud.notificari.domain.RecipientChannel;
import md.mud.notificari.domain.enumeration.Channel;
import md.mud.notificari.errors.RecipientException;
import md.mud.notificari.repository.app.AppRecipientChannelRepository;
import md.mud.notificari.repository.app.AppRecipientRepository;
import md.mud.notificari.service.app.AppDtos.DiscordDirectory;
import md.mud.notificari.service.app.AppDtos.DiscordLink;
import md.mud.notificari.service.app.AppDtos.DiscordMember;
import md.mud.notificari.service.messaging.ChannelSenderRegistry;
import md.mud.notificari.service.messaging.discord.DiscordApiException;
import md.mud.notificari.service.messaging.discord.DiscordClient;
import md.mud.notificari.service.messaging.discord.DiscordResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leaga membrii serverului Discord la destinatarii din aplicatie.
 *
 * De ce exista: botul poate trimite DM doar cuiva de pe acelasi server, iar
 * user id-ul (snowflake) nu se vede in UI-ul Discord obisnuit - trebuie citit
 * din lista membrilor serverului (sau din Developer Mode, scris manual).
 */
@Service
public class DiscordLinkService {

    private static final Logger LOG = LoggerFactory.getLogger(DiscordLinkService.class);
    private static final String PROVIDER = "discord-bot";
    private static final Pattern USER_ID = Pattern.compile("^\\d{17,20}$");

    private final TenantService tenant;
    private final AppRecipientRepository recipients;
    private final AppRecipientChannelRepository channels;
    private final DiscordClient client;
    private final ChannelSenderRegistry registry;

    public DiscordLinkService(
        TenantService tenant,
        AppRecipientRepository recipients,
        AppRecipientChannelRepository channels,
        DiscordClient client,
        ChannelSenderRegistry registry
    ) {
        this.tenant = tenant;
        this.recipients = recipients;
        this.channels = channels;
        this.client = client;
        this.registry = registry;
    }

    @Transactional(readOnly = true)
    public DiscordDirectory directory() {
        requireActiveProvider();
        if (client.guildId().isBlank()) {
            throw RecipientException.discordGuildMissing();
        }
        Long orgId = tenant.currentOrganization().getId();
        Map<String, RecipientChannel> linked = linkedByAddress(orgId);

        DiscordResponses.User bot;
        String guildName;
        List<DiscordResponses.Member> members;
        try {
            bot = client.botUser();
            guildName = client.guildName();
            members = client.members();
        } catch (DiscordApiException e) {
            throw RecipientException.discordUnavailable(e.message());
        } catch (RuntimeException e) {
            LOG.warn("Citirea membrilor Discord a eșuat: {}", e.getMessage());
            throw RecipientException.discordUnavailable(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }

        String inviteUrl = bot == null || bot.id() == null
            ? null
            : "https://discord.com/oauth2/authorize?client_id=" + bot.id() + "&scope=bot&permissions=0";
        return new DiscordDirectory(guildName, inviteUrl, members(members, linked));
    }

    @Transactional
    public void link(DiscordLink request) {
        requireActiveProvider();
        String userId = request.userId() == null ? "" : request.userId().trim();
        if (!USER_ID.matcher(userId).matches()) {
            throw RecipientException.invalidDiscordId();
        }
        Long orgId = tenant.currentOrganization().getId();
        Recipient recipient = recipients
            .findByIdAndOrganizationId(request.recipientId(), orgId)
            .orElseThrow(() -> RecipientException.notFound(request.recipientId()));

        RecipientChannel existing = linkedByAddress(orgId).get(userId);
        if (existing != null && !existing.getRecipient().getId().equals(recipient.getId())) {
            throw RecipientException.discordIdAlreadyLinked(fullName(existing.getRecipient()));
        }

        RecipientChannel row = channels
            .findByRecipientId(recipient.getId())
            .stream()
            .filter(c -> c.getChannel() == Channel.DISCORD)
            .findFirst()
            .orElseGet(() -> new RecipientChannel().channel(Channel.DISCORD).recipient(recipient));
        // verified = true: user id-ul nu e o ghicitoare, vine din lista membrilor serverului (sau e verificat manual de admin).
        channels.save(row.address(userId).active(true).verified(true));
        LOG.debug("User Discord {} legat la destinatarul {}", userId, recipient.getId());
    }

    /**
     * Fara provider real nu exista bot, deci nici membri. Raspundem cu o eroare
     * clara in loc sa intoarcem o lista goala, care ar arata ca "serverul e gol".
     */
    private void requireActiveProvider() {
        String active = registry.activeProviders().get(Channel.DISCORD);
        if (!PROVIDER.equals(active)) {
            throw RecipientException.discordNotActive(active);
        }
    }

    private Map<String, RecipientChannel> linkedByAddress(Long orgId) {
        Map<String, RecipientChannel> byAddress = new HashMap<>();
        for (RecipientChannel row : channels.findAllForOrganizationAndChannel(orgId, Channel.DISCORD)) {
            if (row.getAddress() != null && !row.getAddress().isBlank()) {
                byAddress.put(row.getAddress().trim(), row);
            }
        }
        return byAddress;
    }

    private static List<DiscordMember> members(List<DiscordResponses.Member> members, Map<String, RecipientChannel> linked) {
        List<DiscordMember> out = new ArrayList<>();
        for (DiscordResponses.Member member : members) {
            if (member.user() == null || member.user().id() == null) {
                continue;
            }
            String userId = member.user().id();
            RecipientChannel existing = linked.get(userId);
            String name = member.nick() != null && !member.nick().isBlank()
                ? member.nick()
                : member.user().globalName() != null && !member.user().globalName().isBlank()
                    ? member.user().globalName()
                    : member.user().username();
            out.add(
                new DiscordMember(
                    userId,
                    name,
                    member.user().username(),
                    existing == null ? null : existing.getRecipient().getId(),
                    existing == null ? null : fullName(existing.getRecipient())
                )
            );
        }
        return out;
    }

    private static String fullName(Recipient r) {
        String name = ((r.getFirstName() == null ? "" : r.getFirstName()) + " " + (r.getLastName() == null ? "" : r.getLastName())).trim();
        return name.isEmpty() ? r.getEmail() : name;
    }
}
