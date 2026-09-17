package md.mud.notificari.service.app;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import md.mud.notificari.config.ApplicationProperties;
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
import md.mud.notificari.errors.GroupException;
import md.mud.notificari.errors.MessageException;
import md.mud.notificari.errors.MessageTemplateException;
import md.mud.notificari.errors.RecipientException;
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
import md.mud.notificari.service.app.AppDtos.GroupUpsert;
import md.mud.notificari.service.app.AppDtos.GroupView;
import md.mud.notificari.service.app.AppDtos.MessageDetail;
import md.mud.notificari.service.app.AppDtos.MessageView;
import md.mud.notificari.service.app.AppDtos.OverviewView;
import md.mud.notificari.service.app.AppDtos.RecipientUpsert;
import md.mud.notificari.service.app.AppDtos.RecipientView;
import md.mud.notificari.service.app.AppDtos.SendPayload;
import md.mud.notificari.service.app.AppDtos.SendResult;
import md.mud.notificari.service.app.AppDtos.TemplateView;
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
    private final ApplicationProperties properties;

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
        ApplicationProperties properties
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
        this.properties = properties;
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
        Message m = messages.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> MessageException.notFound(id));
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
            recipients.countByOrganizationIdAndArchivedAtIsNull(org),
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
            .findByOrganizationIdAndEmailIgnoreCaseAndArchivedAtIsNull(org.getId(), email)
            .ifPresent(r -> {
                throw RecipientException.emailAlreadyUsed();
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
        Recipient r = recipients.findByIdAndOrganizationId(id, org.getId()).orElseThrow(() -> RecipientException.notFound(id));
        String email = required(in.email(), "Emailul");
        recipients
            .findByOrganizationIdAndEmailIgnoreCaseAndArchivedAtIsNull(org.getId(), email)
            .filter(other -> !other.getId().equals(id))
            .ifPresent(other -> {
                throw RecipientException.emailAlreadyUsed();
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

    /**
     * Sterge destinatarul daca nu a primit nimic; altfel il arhiveaza, fiindca
     * istoricul livrarilor trimite catre randul lui cu FK obligatoriu. In ambele
     * cazuri dispare din toate ecranele.
     */
    public void deleteRecipient(Long id) {
        Recipient r = recipients.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> RecipientException.notFound(id));
        if (deliveries.existsByRecipientId(r.getId())) {
            recipients.save(r.archivedAt(Instant.now()));
            return;
        }
        recipientChannels.deleteByRecipientId(r.getId());
        recipients.delete(r);
    }

    /**
     * Bulk import: a row whose email already exists is skipped, not rejected.
     *
     * Un chat Telegram deja legat la altcineva nu costa insa toata linia: omul
     * intra fara Telegram. Frontendul goleste celula inainte de a trimite, deci
     * aici ajung doar cursele (doi utilizatori care importa in acelasi timp).
     */
    public List<RecipientView> importRecipients(List<RecipientUpsert> rows) {
        List<RecipientView> added = new ArrayList<>();
        for (RecipientUpsert row : rows) {
            try {
                added.add(createRecipient(freeTakenAddresses(row)));
            } catch (IllegalArgumentException | RecipientException e) {
                LOG.debug("Linie sarita la import: {}", e.getMessage());
            }
        }
        return added;
    }

    /** Adresele deja luate se sterg din linie inainte de a crea ceva, ca sa nu ramana un destinatar pe jumatate scris. */
    private RecipientUpsert freeTakenAddresses(RecipientUpsert row) {
        return new RecipientUpsert(
            row.firstName(),
            row.lastName(),
            row.email(),
            row.group(),
            freeAddress(Channel.TELEGRAM, row.telegramChatId()),
            freeAddress(Channel.DISCORD, row.discordUserId())
        );
    }

    private String freeAddress(Channel channel, String address) {
        String trimmed = address == null ? "" : address.trim();
        return trimmed.isEmpty() || addressOwner(channel, trimmed, null).isEmpty() ? address : null;
    }

    // ============================================================ groups CRUD

    public GroupView createGroup(GroupUpsert in) {
        Organization org = tenant.currentOrganization();
        String name = required(in.name(), "Numele grupului");
        if (groups.findByOrganizationIdAndNameIgnoreCase(org.getId(), name).isPresent()) {
            throw GroupException.nameAlreadyUsed();
        }
        RecipientGroup g = groups.save(new RecipientGroup().name(name).organization(org));
        return new GroupView(g.getId(), g.getName(), 0);
    }

    public GroupView updateGroup(Long id, GroupUpsert in) {
        Long org = orgId();
        RecipientGroup g = groups.findByIdAndOrganizationId(id, org).orElseThrow(() -> GroupException.notFound(id));
        String name = required(in.name(), "Numele grupului");
        groups
            .findByOrganizationIdAndNameIgnoreCase(org, name)
            .filter(other -> !other.getId().equals(id))
            .ifPresent(other -> {
                throw GroupException.nameAlreadyUsed();
            });
        g.name(name);
        groups.save(g);
        return new GroupView(g.getId(), g.getName(), recipients.countByRecipientGroupId(id));
    }

    /**
     * FK-ul de pe Recipient e obligatoriu (optional = false): un grup cu destinatari nu poate
     * fi sters fara sa-i strice pe cei ramasi fara grup. Numaram si arhivatii, nu doar cei vii -
     * tot tin FK-ul spre grup.
     */
    public void deleteGroup(Long id) {
        RecipientGroup g = groups.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> GroupException.notFound(id));
        long count = recipients.countByRecipientGroupId(id);
        if (count > 0) {
            throw GroupException.hasRecipients(count);
        }
        groups.delete(g);
    }

    // ========================================================= templates CRUD

    public TemplateView createTemplate(TemplateView in) {
        Organization org = tenant.currentOrganization();
        String name = required(in.name(), "Numele sablonului");
        if (templates.existsByOrganizationIdAndNameIgnoreCase(org.getId(), name)) {
            throw MessageTemplateException.nameAlreadyUsed();
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

    public TemplateView updateTemplate(Long id, TemplateView in) {
        MessageTemplate t = templates.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> MessageTemplateException.notFound(id));
        String name = required(in.name(), "Numele sablonului");
        if (templates.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(orgId(), name, id)) {
            throw MessageTemplateException.nameAlreadyUsed();
        }
        t.name(name)
            .description(blankTo(in.description(), "Sablon creat de tine."))
            .subject(blankTo(in.subject(), name))
            .body(nullToEmpty(in.body()));
        templates.save(t);
        return new TemplateView(t.getId(), t.getName(), t.getDescription(), t.getSubject(), t.getBody());
    }

    public void deleteTemplate(Long id) {
        templates.delete(templates.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> MessageTemplateException.notFound(id)));
    }

    public List<TemplateView> importTemplates(List<TemplateView> rows) {
        List<TemplateView> added = new ArrayList<>();
        for (TemplateView row : rows) {
            try {
                added.add(createTemplate(row));
            } catch (IllegalArgumentException | MessageTemplateException e) {
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
        Message m = messages.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> MessageException.notFound(id));
        messageChannels.deleteAll(m.getChannelses());
        attachments.deleteAll(m.getAttachmentses());
        messages.delete(m);
    }

    /**
     * Pune mesajul in coada: un rand MessageRecipient per (destinatar, canal).
     * Nu face niciun apel de retea, deci tranzactia ramane scurta - trimiterea
     * propriu-zisa e treaba dispecerului.
     *
     * Apelantul e {@link SendCoordinator}, care in modul sincron scurge coada
     * imediat dupa commit.
     */
    public SendResult send(SendPayload payload) {
        if (!properties.getMessaging().isEnabled()) {
            throw MessageException.sendingDisabled();
        }
        Organization org = tenant.currentOrganization();
        ComposePayload compose = payload.message();
        List<Channel> wanted = parseChannels(compose.channels());
        if (wanted.isEmpty()) {
            throw MessageException.noChannelSelected();
        }
        List<Long> ids = payload.recipientIds() == null ? List.of() : payload.recipientIds();
        if (ids.isEmpty()) {
            throw MessageException.noRecipientSelected();
        }
        List<Recipient> targets = recipients.findByOrganizationIdAndArchivedAtIsNullAndIdIn(org.getId(), ids);
        if (targets.isEmpty()) {
            throw MessageException.noRecipientSelected();
        }

        Message message = persistMessage(compose, MessageStatus.QUEUED, targets.size());
        Map<Long, List<String>> overrides = payload.channelOverrides() == null ? Map.of() : payload.channelOverrides();
        Instant now = Instant.now();

        int queued = 0;
        int skipped = 0;

        for (Recipient r : targets) {
            List<Channel> allowed = overrides.containsKey(r.getId()) ? parseChannels(overrides.get(r.getId())) : wanted;
            for (Channel ch : allowed) {
                if (addressFor(r, ch).isEmpty()) {
                    // Canal neconfigurat pentru acest destinatar: nu e o eroare de
                    // livrare, e rezultatul selectiei de la pasul 3. Se noteaza SKIPPED.
                    deliveries.save(new MessageRecipient().message(message).recipient(r).channel(ch).status(DeliveryStatus.SKIPPED));
                    skipped++;
                    continue;
                }
                deliveries.save(
                    new MessageRecipient()
                        .message(message)
                        .recipient(r)
                        .channel(ch)
                        .status(DeliveryStatus.PENDING)
                        .attemptCount(0)
                        .nextAttemptAt(now)
                );
                queued++;
            }
        }

        return new SendResult(message.getId(), MessageStatus.QUEUED.name(), queued, 0, 0, skipped, targets.size());
    }

    /** Starea curenta a unei trimiteri - suficient de ieftina pentru poll la cateva secunde. */
    @Transactional(readOnly = true)
    public SendResult sendStatus(Long messageId) {
        Message message = messages.findByIdAndOrganizationId(messageId, orgId()).orElseThrow(() -> MessageException.notFound(messageId));
        List<DeliveryStatus> statuses = deliveries.statusesForMessage(messageId);
        int queued = (int) statuses.stream().filter(DeliveryStatus::isInFlight).count();
        int delivered = (int) statuses.stream().filter(DeliveryStatus::isSuccess).count();
        int failed = (int) statuses.stream().filter(s -> s == DeliveryStatus.FAILED).count();
        int skipped = (int) statuses.stream().filter(s -> s == DeliveryStatus.SKIPPED).count();
        return new SendResult(
            message.getId(),
            message.getStatus().name(),
            queued,
            delivered,
            failed,
            skipped,
            message.getRecipientCount() == null ? 0 : message.getRecipientCount()
        );
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
        // Randurile se rescriu din formular, dar "verified" nu se pierde daca adresa
        // a ramas aceeasi: un chat Telegram legat din ecranul de conectare e verificat,
        // iar o simpla editare de nume nu are de ce sa-l retrogradeze.
        Map<Channel, RecipientChannel> before = r.getId() == null
            ? Map.of()
            : recipientChannels
                .findByRecipientId(r.getId())
                .stream()
                .collect(Collectors.toMap(RecipientChannel::getChannel, c -> c, (a, b) -> a, () -> new EnumMap<>(Channel.class)));
        recipientChannels.deleteByRecipientId(r.getId());
        recipientChannels.save(
            new RecipientChannel().channel(Channel.EMAIL).address(r.getEmail()).active(true).verified(true).recipient(r)
        );
        if (in.telegramChatId() != null && !in.telegramChatId().isBlank()) {
            String address = in.telegramChatId().trim();
            addressOwner(Channel.TELEGRAM, address, r.getId()).ifPresent(other -> {
                throw RecipientException.chatIdAlreadyLinked(fullName(other));
            });
            recipientChannels.save(
                new RecipientChannel()
                    .channel(Channel.TELEGRAM)
                    .address(address)
                    .active(true)
                    .verified(wasVerified(before.get(Channel.TELEGRAM), address))
                    .recipient(r)
            );
        }
        if (in.discordUserId() != null && !in.discordUserId().isBlank()) {
            String address = in.discordUserId().trim();
            addressOwner(Channel.DISCORD, address, r.getId()).ifPresent(other -> {
                throw RecipientException.discordIdAlreadyLinked(fullName(other));
            });
            recipientChannels.save(
                new RecipientChannel()
                    .channel(Channel.DISCORD)
                    .address(address)
                    .active(true)
                    .verified(wasVerified(before.get(Channel.DISCORD), address))
                    .recipient(r)
            );
        }
    }

    /**
     * Destinatarul care are deja aceasta adresa pe acest canal, daca e altul decat
     * {@code selfId} ({@code null} inseamna "oricine conteaza").
     *
     * Aceeasi adresa nu poate fi a doua persoane: mesajul ar ajunge de doua ori
     * la una si deloc la cealalta. Regula exista si la legarea din ecranele de
     * conectare ({@link TelegramLinkService#link}, {@link DiscordLinkService#link}),
     * dar lipsea pe calea formularului si a importului.
     */
    private Optional<Recipient> addressOwner(Channel channel, String address, Long selfId) {
        return recipientChannels
            .findAllForOrganizationAndChannel(orgId(), channel)
            .stream()
            .filter(c -> address.equals(c.getAddress() == null ? null : c.getAddress().trim()))
            .map(RecipientChannel::getRecipient)
            .filter(other -> !other.getId().equals(selfId))
            .findFirst();
    }

    private static boolean wasVerified(RecipientChannel previous, String address) {
        return previous != null && Boolean.TRUE.equals(previous.getVerified()) && address.equals(previous.getAddress());
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
            addressOf(r, Channel.TELEGRAM),
            chans,
            addressOf(r, Channel.DISCORD)
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
}
