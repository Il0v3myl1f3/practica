package md.mud.notificari.service.app;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import md.mud.notificari.domain.Recipient;
import md.mud.notificari.domain.RecipientChannel;
import md.mud.notificari.domain.enumeration.Channel;
import md.mud.notificari.errors.RecipientException;
import md.mud.notificari.repository.app.AppRecipientChannelRepository;
import md.mud.notificari.repository.app.AppRecipientRepository;
import md.mud.notificari.service.app.AppDtos.TelegramContact;
import md.mud.notificari.service.app.AppDtos.TelegramDirectory;
import md.mud.notificari.service.app.AppDtos.TelegramLink;
import md.mud.notificari.service.messaging.ChannelSenderRegistry;
import md.mud.notificari.service.messaging.telegram.TelegramApiException;
import md.mud.notificari.service.messaging.telegram.TelegramClient;
import md.mud.notificari.service.messaging.telegram.TelegramUpdates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leaga oamenii care au scris botului la destinatarii din aplicatie.
 *
 * De ce exista: un bot Telegram <b>nu poate scrie primul</b> nimanui, iar Bot API
 * nu accepta {@code @username} pentru persoane - are nevoie de {@code chat_id}
 * numeric. Singura sursa de chat ID-uri e lista mesajelor primite de bot.
 */
@Service
public class TelegramLinkService {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramLinkService.class);
    private static final String PROVIDER = "telegram-bot";

    private final TenantService tenant;
    private final AppRecipientRepository recipients;
    private final AppRecipientChannelRepository channels;
    private final TelegramClient client;
    private final ChannelSenderRegistry registry;

    public TelegramLinkService(
        TenantService tenant,
        AppRecipientRepository recipients,
        AppRecipientChannelRepository channels,
        TelegramClient client,
        ChannelSenderRegistry registry
    ) {
        this.tenant = tenant;
        this.recipients = recipients;
        this.channels = channels;
        this.client = client;
        this.registry = registry;
    }

    @Transactional(readOnly = true)
    public TelegramDirectory directory() {
        requireActiveProvider();
        Long orgId = tenant.currentOrganization().getId();
        Map<String, RecipientChannel> linked = linkedByAddress(orgId);

        String botUsername;
        List<TelegramUpdates.Update> updates;
        try {
            botUsername = client.botUsername();
            updates = client.updates();
        } catch (TelegramApiException e) {
            throw RecipientException.telegramUnavailable(e.description());
        } catch (RuntimeException e) {
            LOG.warn("Citirea contactelor Telegram a eșuat: {}", e.getMessage());
            throw RecipientException.telegramUnavailable(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }

        List<TelegramContact> contacts = contacts(updates, linked);
        return new TelegramDirectory(botUsername, contacts);
    }

    @Transactional
    public void link(TelegramLink request) {
        requireActiveProvider();
        String chatId = request.chatId() == null ? "" : request.chatId().trim();
        if (chatId.isEmpty()) {
            throw RecipientException.invalidChatId();
        }
        Long orgId = tenant.currentOrganization().getId();
        Recipient recipient = recipients
            .findByIdAndOrganizationId(request.recipientId(), orgId)
            .orElseThrow(() -> RecipientException.notFound(request.recipientId()));

        RecipientChannel existing = linkedByAddress(orgId).get(chatId);
        if (existing != null && !existing.getRecipient().getId().equals(recipient.getId())) {
            throw RecipientException.chatIdAlreadyLinked(fullName(existing.getRecipient()));
        }

        RecipientChannel row = channels
            .findByRecipientId(recipient.getId())
            .stream()
            .filter(c -> c.getChannel() == Channel.TELEGRAM)
            .findFirst()
            .orElseGet(() -> new RecipientChannel().channel(Channel.TELEGRAM).recipient(recipient));
        // verified = true: chat ID-ul nu e scris de mana, vine din mesajul primit de bot.
        channels.save(row.address(chatId).active(true).verified(true));
        LOG.debug("Chat {} legat la destinatarul {}", chatId, recipient.getId());
    }

    /**
     * Fara provider real nu exista bot, deci nici contacte. Raspundem cu o eroare
     * clara in loc sa intoarcem o lista goala, care ar arata ca "nimeni n-a scris".
     */
    private void requireActiveProvider() {
        String active = registry.activeProviders().get(Channel.TELEGRAM);
        if (!PROVIDER.equals(active)) {
            throw RecipientException.telegramNotActive(active);
        }
    }

    private Map<String, RecipientChannel> linkedByAddress(Long orgId) {
        Map<String, RecipientChannel> byAddress = new HashMap<>();
        for (RecipientChannel row : channels.findAllForOrganizationAndChannel(orgId, Channel.TELEGRAM)) {
            if (row.getAddress() != null && !row.getAddress().isBlank()) {
                byAddress.put(row.getAddress().trim(), row);
            }
        }
        return byAddress;
    }

    /** Un chat apare o singura data, cu ultimul mesaj primit de la el. */
    private static List<TelegramContact> contacts(List<TelegramUpdates.Update> updates, Map<String, RecipientChannel> linked) {
        Map<String, TelegramContact> byChatId = new LinkedHashMap<>();
        for (TelegramUpdates.Update update : updates) {
            TelegramUpdates.Message message = update.message();
            if (message == null || message.chat() == null || message.chat().id() == null) {
                continue;
            }
            TelegramUpdates.Chat chat = message.chat();
            String chatId = String.valueOf(chat.id());
            Instant at = message.date() == null ? null : Instant.ofEpochSecond(message.date());
            TelegramContact previous = byChatId.get(chatId);
            if (previous != null && previous.lastAt() != null && at != null && previous.lastAt().isAfter(at)) {
                continue;
            }
            RecipientChannel existing = linked.get(chatId);
            byChatId.put(
                chatId,
                new TelegramContact(
                    chatId,
                    chat.displayName(),
                    chat.username(),
                    message.text(),
                    at,
                    existing == null ? null : existing.getRecipient().getId(),
                    existing == null ? null : fullName(existing.getRecipient())
                )
            );
        }
        List<TelegramContact> contacts = new ArrayList<>(byChatId.values());
        contacts.sort(Comparator.comparing(TelegramContact::lastAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return contacts;
    }

    private static String fullName(Recipient r) {
        String name = ((r.getFirstName() == null ? "" : r.getFirstName()) + " " + (r.getLastName() == null ? "" : r.getLastName())).trim();
        return name.isEmpty() ? r.getEmail() : name;
    }
}
