package md.mud.notificari.service.app;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Payloads for the app-facing API. Kept together: they are plain data, not domain. */
public final class AppDtos {

    private AppDtos() {}

    // ---------------------------------------------------------------- views

    public record RecipientView(
        Long id,
        String firstName,
        String lastName,
        String name,
        String email,
        String group,
        String telegramChatId,
        List<String> channels,
        String discordUserId
    ) {}

    public record GroupView(Long id, String name, long count) {}

    public record TemplateView(Long id, String name, String description, String subject, String body) {}

    public record MessageView(
        Long id,
        String subject,
        List<String> channels,
        int recipientCount,
        Instant createdAt,
        Instant sentAt,
        String status,
        int images
    ) {}

    public record MessageDetail(MessageView summary, String bodyHtml, List<String> recipientNames) {}

    public record OverviewView(
        long totalSent,
        long recipients,
        long groups,
        long drafts,
        long averageReach,
        long fullyDelivered,
        List<GroupView> groupDistribution
    ) {}

    /**
     * Starea unei trimiteri. Cat timp {@code status} e QUEUED, {@code queued} e
     * numarul de livrari inca in lucru si restul cresc pe masura ce coada se
     * scurge; frontendul reinterogheaza pana cand statusul nu mai e QUEUED.
     */
    public record SendResult(
        Long messageId,
        String status,
        int queued,
        int delivered,
        int failed,
        int skipped,
        int recipientCount
    ) {}

    /**
     * Contactele botului de Telegram, adica oamenii care i-au scris. Un bot nu
     * poate scrie primul nimanui, deci asta e singura cale de a afla un chat ID.
     *
     * {@code botUsername} e pentru linkul t.me/... pe care il dai oamenilor;
     * {@code recipientId} e nenul cand chat ID-ul e deja legat la un destinatar.
     */
    public record TelegramDirectory(String botUsername, List<TelegramContact> contacts) {}

    public record TelegramContact(
        String chatId,
        String name,
        String username,
        String lastMessage,
        Instant lastAt,
        Long recipientId,
        String recipientName
    ) {}

    // ------------------------------------------------------------- requests

    public record RecipientUpsert(
        String firstName,
        String lastName,
        String email,
        String group,
        String telegramChatId,
        String discordUserId
    ) {}

    public record GroupUpsert(String name) {}

    public record AttachmentPayload(String fileName, String contentType, String dataBase64) {}

    public record ComposePayload(
        String subject,
        String bodyHtml,
        List<String> channels,
        Long templateId,
        List<AttachmentPayload> attachments
    ) {}

    /**
     * channelOverrides maps a recipient id to the subset of channels the message
     * should actually go out on — the per-person resolution of a channel conflict.
     */
    public record SendPayload(
        ComposePayload message,
        List<Long> recipientIds,
        Map<Long, List<String>> channelOverrides
    ) {}

    public record TelegramLink(String chatId, Long recipientId) {}

    /**
     * Membrii serverului Discord, pentru ecranul de conectare. {@code inviteUrl} e
     * linkul de invitare al botului, construit din id-ul lui; {@code recipientId} e
     * nenul cand user id-ul e deja legat la un destinatar.
     */
    public record DiscordDirectory(String guildName, String inviteUrl, List<DiscordMember> members) {}

    public record DiscordMember(String userId, String name, String username, Long recipientId, String recipientName) {}

    public record DiscordLink(String userId, Long recipientId) {}
}
