package md.mud.notificari.service.app;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import md.mud.notificari.domain.Message;
import md.mud.notificari.domain.MessageAttachment;
import md.mud.notificari.domain.MessageChannel;
import md.mud.notificari.domain.MessageRecipient;
import md.mud.notificari.domain.MessageTemplate;
import md.mud.notificari.domain.Organization;
import md.mud.notificari.domain.Recipient;
import md.mud.notificari.domain.RecipientChannel;
import md.mud.notificari.domain.RecipientGroup;
import md.mud.notificari.domain.enumeration.Channel;
import md.mud.notificari.domain.enumeration.DeliveryStatus;
import md.mud.notificari.domain.enumeration.MessageStatus;
import md.mud.notificari.repository.MessageAttachmentRepository;
import md.mud.notificari.repository.MessageChannelRepository;
import md.mud.notificari.repository.app.AppMessageRecipientRepository;
import md.mud.notificari.repository.app.AppMessageRepository;
import md.mud.notificari.repository.app.AppMessageTemplateRepository;
import md.mud.notificari.repository.app.AppRecipientChannelRepository;
import md.mud.notificari.repository.app.AppRecipientGroupRepository;
import md.mud.notificari.repository.app.AppRecipientRepository;
import md.mud.notificari.service.app.AppDtos.AttachmentPayload;
import md.mud.notificari.service.app.AppDtos.ComposePayload;
import md.mud.notificari.service.app.AppDtos.GroupView;
import md.mud.notificari.service.app.AppDtos.MessageDetail;
import md.mud.notificari.service.app.AppDtos.MessageView;
import md.mud.notificari.service.app.AppDtos.OverviewView;
import md.mud.notificari.service.app.AppDtos.RecipientUpsert;
import md.mud.notificari.service.app.AppDtos.RecipientView;
import md.mud.notificari.service.app.AppDtos.SendPayload;
import md.mud.notificari.service.app.AppDtos.SendResult;
import md.mud.notificari.service.app.AppDtos.TemplateView;
import md.mud.notificari.service.messaging.ChannelSender;
import md.mud.notificari.service.messaging.Delivery;
import md.mud.notificari.service.messaging.SendOutcome;
import md.mud.notificari.service.messaging.VariableRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everything the UI needs, scoped to the organization of the current user.
 *
 * The generated CRUD resources stay untouched for admin use; this is the
 * app-facing surface, shaped after the screens rather than after the tables.
 */
@Service
@Transactional
public class AppService {

    private static final Logger LOG = LoggerFactory.getLogger(AppService.class);

    private final TenantService tenant;
    private final AppRecipientRepository recipients;
    private final AppRecipientGroupRepository groups;
    private final AppRecipientChannelRepository recipientChannels;
    private final AppMessageTemplateRepository templates;
    private final AppMessageRepository messages;
    private final AppMessageRecipientRepository deliveries;
    private final MessageChannelRepository messageChannels;
    private final MessageAttachmentRepository attachments;
    private final VariableRenderer renderer;
    private final Map<Channel, ChannelSender> senders = new EnumMap<>(Channel.class);

    public AppService(
        TenantService tenant,
        AppRecipientRepository recipients,
        AppRecipientGroupRepository groups,
        AppRecipientChannelRepository recipientChannels,
        AppMessageTemplateRepository templates,
        AppMessageRepository messages,
        AppMessageRecipientRepository deliveries,
        MessageChannelRepository messageChannels,
        MessageAttachmentRepository attachments,
        VariableRenderer renderer,
        List<ChannelSender> senderBeans
    ) {
        this.tenant = tenant;
        this.recipients = recipients;
        this.groups = groups;
        this.recipientChannels = recipientChannels;
        this.templates = templates;
        this.messages = messages;
        this.deliveries = deliveries;
        this.messageChannels = messageChannels;
        this.attachments = attachments;
        this.renderer = renderer;
        senderBeans.forEach(s -> this.senders.put(s.channel(), s));
    }

    // ================================================================== reads

    @Transactional(readOnly = true)
    public List<RecipientView> recipients() {
        return recipients.findAllForOrganization(orgId()).stream().map(AppService::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<GroupView> groups() {
        Long org = orgId();
        Map<Long, Long> counts = recipients
            .findAllForOrganization(org)
            .stream()
            .filter(r -> r.getRecipientGroup() != null)
            .collect(Collectors.groupingBy(r -> r.getRecipientGroup().getId(), Collectors.counting()));
        return groups
            .findByOrganizationIdOrderByNameAsc(org)
            .stream()
            .map(g -> new GroupView(g.getId(), g.getName(), counts.getOrDefault(g.getId(), 0L)))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TemplateView> templates() {
        return templates
            .findByOrganizationIdOrderByNameAsc(orgId())
            .stream()
            .map(t -> new TemplateView(t.getId(), t.getName(), t.getDescription(), t.getSubject(), t.getBody()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageView> sentMessages() {
        return messages.findAllForOrganizationExcept(orgId(), MessageStatus.DRAFT).stream().map(AppService::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageView> drafts() {
        return messages.findAllForOrganization(orgId(), MessageStatus.DRAFT).stream().map(AppService::toView).toList();
    }

    @Transactional(readOnly = true)
    public MessageDetail message(Long id) {
        Message m = messages.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> notFound("Mesajul"));
        List<String> names = deliveries
            .findAllForMessage(m.getId())
            .stream()
            .filter(d -> d.getRecipient() != null)
            .map(d -> fullName(d.getRecipient()))
            .distinct()
            .sorted()
            .toList();
        return new MessageDetail(toView(m), m.getBodyHtml(), names);
    }

    @Transactional(readOnly = true)
    public OverviewView overview() {
        Long org = orgId();
        List<Message> sent = messages.findAllForOrganizationExcept(org, MessageStatus.DRAFT);
        long reach = sent.isEmpty()
            ? 0
            : Math.round(sent.stream().mapToInt(m -> m.getRecipientCount() == null ? 0 : m.getRecipientCount()).average().orElse(0));
        long delivered = sent.stream().filter(m -> m.getStatus() == MessageStatus.SENT).count();
        return new OverviewView(
            sent.size(),
            recipients.countByOrganizationId(org),
            groups.countByOrganizationId(org),
            messages.countByOrganizationIdAndStatus(org, MessageStatus.DRAFT),
            reach,
            delivered,
            groups()
        );
    }

    // ======================================================== recipients CRUD

    public RecipientView createRecipient(RecipientUpsert in) {
        Organization org = tenant.currentOrganization();
        String email = required(in.email(), "Emailul");
        recipients
            .findByOrganizationIdAndEmailIgnoreCase(org.getId(), email)
            .ifPresent(r -> {
                throw new IllegalArgumentException("Un alt destinatar are deja acest email.");
            });
        Recipient saved = recipients.save(
            new Recipient()
                .firstName(required(in.firstName(), "Prenumele"))
                .lastName(required(in.lastName(), "Numele"))
                .email(email)
                .createdAt(Instant.now())
                .organization(org)
                .recipientGroup(resolveGroup(org, in.group()))
        );
        syncChannels(saved, in);
        return toView(recipients.findByIdAndOrganizationId(saved.getId(), org.getId()).orElseThrow());
    }

    public RecipientView updateRecipient(Long id, RecipientUpsert in) {
        Organization org = tenant.currentOrganization();
        Recipient r = recipients.findByIdAndOrganizationId(id, org.getId()).orElseThrow(() -> notFound("Destinatarul"));
        String email = required(in.email(), "Emailul");
        recipients
            .findByOrganizationIdAndEmailIgnoreCase(org.getId(), email)
            .filter(other -> !other.getId().equals(id))
            .ifPresent(other -> {
                throw new IllegalArgumentException("Un alt destinatar are deja acest email.");
            });
        r
            .firstName(required(in.firstName(), "Prenumele"))
            .lastName(required(in.lastName(), "Numele"))
            .email(email)
            .recipientGroup(resolveGroup(org, in.group()));
        recipients.save(r);
        syncChannels(r, in);
        return toView(recipients.findByIdAndOrganizationId(id, org.getId()).orElseThrow());
    }

    public void deleteRecipient(Long id) {
        Recipient r = recipients.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> notFound("Destinatarul"));
        recipientChannels.deleteByRecipientId(r.getId());
        recipients.delete(r);
    }

    /** Bulk import: a row whose email already exists is skipped, not rejected. */
    public List<RecipientView> importRecipients(List<RecipientUpsert> rows) {
        List<RecipientView> added = new ArrayList<>();
        for (RecipientUpsert row : rows) {
            try {
                added.add(createRecipient(row));
            } catch (IllegalArgumentException e) {
                LOG.debug("Linie sarita la import: {}", e.getMessage());
            }
        }
        return added;
    }

    // ========================================================= templates CRUD

    public TemplateView createTemplate(TemplateView in) {
        Organization org = tenant.currentOrganization();
        String name = required(in.name(), "Numele sablonului");
        if (templates.existsByOrganizationIdAndNameIgnoreCase(org.getId(), name)) {
            throw new IllegalArgumentException("Exista deja un sablon cu acest nume.");
        }
        MessageTemplate t = templates.save(
            new MessageTemplate()
                .name(name)
                .description(blankTo(in.description(), "Sablon creat de tine."))
                .subject(blankTo(in.subject(), name))
                .body(nullToEmpty(in.body()))
                .organization(org)
        );
        return new TemplateView(t.getId(), t.getName(), t.getDescription(), t.getSubject(), t.getBody());
    }

    public void deleteTemplate(Long id) {
        templates.delete(templates.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> notFound("Sablonul")));
    }

    public List<TemplateView> importTemplates(List<TemplateView> rows) {
        List<TemplateView> added = new ArrayList<>();
        for (TemplateView row : rows) {
            try {
                added.add(createTemplate(row));
            } catch (IllegalArgumentException e) {
                LOG.debug("Sablon sarit la import: {}", e.getMessage());
            }
        }
        return added;
    }

    // ============================================================ drafts/send

    public MessageView saveDraft(ComposePayload in) {
        return toView(persistMessage(in, MessageStatus.DRAFT, 0));
    }

    public void deleteDraft(Long id) {
        Message m = messages.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> notFound("Ciorna"));
        messageChannels.deleteAll(m.getChannelses());
        attachments.deleteAll(m.getAttachmentses());
        messages.delete(m);
    }

    public SendResult send(SendPayload payload) {
        Organization org = tenant.currentOrganization();
        ComposePayload compose = payload.message();
        List<Channel> wanted = parseChannels(compose.channels());
        if (wanted.isEmpty()) {
            throw new IllegalArgumentException("Alege cel putin un canal.");
        }
        List<Long> ids = payload.recipientIds() == null ? List.of() : payload.recipientIds();
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Bifeaza cel putin un destinatar.");
        }
        List<Recipient> targets = recipients.findByOrganizationIdAndIdIn(org.getId(), ids);

        Message message = persistMessage(compose, MessageStatus.QUEUED, targets.size());
        List<Delivery.Attachment> files = decode(compose.attachments());
        Map<Long, List<String>> overrides = payload.channelOverrides() == null ? Map.of() : payload.channelOverrides();

        int delivered = 0;
        int failed = 0;
        Set<Long> reached = new LinkedHashSet<>();

        for (Recipient r : targets) {
            List<Channel> allowed = overrides.containsKey(r.getId()) ? parseChannels(overrides.get(r.getId())) : wanted;
            for (Channel ch : allowed) {
                Optional<String> address = addressFor(r, ch);
                if (address.isEmpty()) {
                    // Canal neconfigurat pentru acest destinatar: nu e o eroare de
                    // livrare, e rezultatul selectiei de la pasul 3. Se noteaza SKIPPED.
                    deliveries.save(new MessageRecipient().message(message).recipient(r).channel(ch).status(DeliveryStatus.SKIPPED));
                    continue;
                }
                String subject = renderer.render(nullToEmpty(compose.subject()), r);
                String body = renderer.render(nullToEmpty(compose.bodyHtml()), r);
                SendOutcome outcome = senders.get(ch).send(new Delivery(address.get(), fullName(r), subject, body, files));
                MessageRecipient row = new MessageRecipient()
                    .message(message)
                    .recipient(r)
                    .channel(ch)
                    .status(outcome.ok() ? DeliveryStatus.DELIVERED : DeliveryStatus.FAILED)
                    .providerMessageId(outcome.providerMessageId())
                    .errorMessage(truncate(outcome.error()))
                    .sentAt(Instant.now());
                if (outcome.ok()) {
                    row.deliveredAt(Instant.now());
                    delivered++;
                    reached.add(r.getId());
                } else {
                    failed++;
                }
                deliveries.save(row);
            }
        }

        MessageStatus status = failed == 0 ? MessageStatus.SENT : (delivered == 0 ? MessageStatus.FAILED : MessageStatus.PARTIAL);
        message.status(status).sentAt(Instant.now()).recipientCount(reached.size());
        messages.save(message);
        return new SendResult(message.getId(), status.name(), delivered, failed, reached.size());
    }

    // ================================================================ helpers

    private Message persistMessage(ComposePayload in, MessageStatus status, int recipientCount) {
        Organization org = tenant.currentOrganization();
        MessageTemplate template = in.templateId() == null
            ? null
            : templates.findByIdAndOrganizationId(in.templateId(), org.getId()).orElse(null);
        Message m = messages.save(
            new Message()
                .subject(blankTo(in.subject(), "Fara subiect"))
                .bodyHtml(nullToEmpty(in.bodyHtml()))
                .status(status)
                .recipientCount(recipientCount)
                .createdAt(Instant.now())
                .organization(org)
                .template(template)
                .createdBy(tenant.currentUser())
        );
        for (Channel ch : parseChannels(in.channels())) {
            m.getChannelses().add(messageChannels.save(new MessageChannel().channel(ch).message(m)));
        }
        if (in.attachments() != null) {
            for (AttachmentPayload a : in.attachments()) {
                m
                    .getAttachmentses()
                    .add(
                        attachments.save(
                            new MessageAttachment()
                                .fileName(a.fileName())
                                .contentType(a.contentType())
                                .file(Base64.getDecoder().decode(stripDataUrl(a.dataBase64())))
                                .fileContentType(a.contentType())
                                .message(m)
                        )
                    );
            }
        }
        return m;
    }

    private void syncChannels(Recipient r, RecipientUpsert in) {
        recipientChannels.deleteByRecipientId(r.getId());
        recipientChannels.save(
            new RecipientChannel().channel(Channel.EMAIL).address(r.getEmail()).active(true).verified(true).recipient(r)
        );
        if (in.telegramChatId() != null && !in.telegramChatId().isBlank()) {
            recipientChannels.save(
                new RecipientChannel()
                    .channel(Channel.TELEGRAM)
                    .address(in.telegramChatId().trim())
                    .active(true)
                    .verified(false)
                    .recipient(r)
            );
        }
        if (in.phoneNumber() != null && !in.phoneNumber().isBlank()) {
            recipientChannels.save(
                new RecipientChannel()
                    .channel(Channel.WHATSAPP)
                    .address(in.phoneNumber().trim())
                    .active(true)
                    .verified(false)
                    .recipient(r)
            );
        }
    }

    private RecipientGroup resolveGroup(Organization org, String name) {
        String clean = required(name, "Grupul");
        return groups
            .findByOrganizationIdAndNameIgnoreCase(org.getId(), clean)
            .orElseGet(() -> groups.save(new RecipientGroup().name(clean).organization(org)));
    }

    private Optional<String> addressFor(Recipient r, Channel ch) {
        return r
            .getChannelses()
            .stream()
            .filter(c -> c.getChannel() == ch && Boolean.TRUE.equals(c.getActive()))
            .map(RecipientChannel::getAddress)
            .filter(a -> a != null && !a.isBlank())
            .findFirst();
    }

    private List<Delivery.Attachment> decode(List<AttachmentPayload> payloads) {
        if (payloads == null) {
            return List.of();
        }
        return payloads
            .stream()
            .map(a -> new Delivery.Attachment(a.fileName(), a.contentType(), Base64.getDecoder().decode(stripDataUrl(a.dataBase64()))))
            .toList();
    }

    private static String stripDataUrl(String value) {
        if (value == null) {
            return "";
        }
        int comma = value.indexOf(',');
        return value.startsWith("data:") && comma > 0 ? value.substring(comma + 1) : value;
    }

    private Long orgId() {
        return tenant.currentOrganization().getId();
    }

    private static List<Channel> parseChannels(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw
            .stream()
            .filter(s -> s != null && !s.isBlank())
            .map(s -> Channel.valueOf(s.trim().toUpperCase()))
            .distinct()
            .toList();
    }

    private static RecipientView toView(Recipient r) {
        List<String> chans = r
            .getChannelses()
            .stream()
            .filter(c -> Boolean.TRUE.equals(c.getActive()))
            .map(c -> c.getChannel().name())
            .sorted()
            .toList();
        return new RecipientView(
            r.getId(),
            r.getFirstName(),
            r.getLastName(),
            fullName(r),
            r.getEmail(),
            r.getRecipientGroup() == null ? null : r.getRecipientGroup().getName(),
            addressOf(r, Channel.WHATSAPP),
            addressOf(r, Channel.TELEGRAM),
            chans
        );
    }

    private static String addressOf(Recipient r, Channel ch) {
        return r
            .getChannelses()
            .stream()
            .filter(c -> c.getChannel() == ch && Boolean.TRUE.equals(c.getActive()))
            .map(RecipientChannel::getAddress)
            .findFirst()
            .orElse(null);
    }

    private static MessageView toView(Message m) {
        List<String> chans = m.getChannelses().stream().map(c -> c.getChannel().name()).sorted().toList();
        return new MessageView(
            m.getId(),
            m.getSubject(),
            chans,
            m.getRecipientCount() == null ? 0 : m.getRecipientCount(),
            m.getCreatedAt(),
            m.getSentAt(),
            m.getStatus().name(),
            m.getAttachmentses() == null ? 0 : m.getAttachmentses().size()
        );
    }

    private static String fullName(Recipient r) {
        return (nullToEmpty(r.getFirstName()) + " " + nullToEmpty(r.getLastName())).trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " este obligatoriu.");
        }
        return value.trim();
    }

    private static String truncate(String s) {
        return s == null ? null : s.substring(0, Math.min(s.length(), 500));
    }

    private static IllegalArgumentException notFound(String what) {
        return new IllegalArgumentException(what + " nu a fost gasit.");
    }
}
