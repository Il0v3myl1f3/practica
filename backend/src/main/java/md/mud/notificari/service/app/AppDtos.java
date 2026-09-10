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
        String phoneNumber,
        String telegramChatId,
        List<String> channels
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

    public record SendResult(Long messageId, String status, int delivered, int failed, int recipientCount) {}

    // ------------------------------------------------------------- requests

    public record RecipientUpsert(
        String firstName,
        String lastName,
        String email,
        String group,
        String phoneNumber,
        String telegramChatId
    ) {}

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
}
