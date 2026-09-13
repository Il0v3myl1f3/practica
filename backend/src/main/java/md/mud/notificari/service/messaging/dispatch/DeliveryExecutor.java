package md.mud.notificari.service.messaging.dispatch;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import md.mud.notificari.domain.Message;
import md.mud.notificari.domain.MessageRecipient;
import md.mud.notificari.domain.Recipient;
import md.mud.notificari.domain.RecipientChannel;
import md.mud.notificari.domain.enumeration.Channel;
import md.mud.notificari.domain.enumeration.DeliveryStatus;
import md.mud.notificari.repository.app.AppMessageRecipientRepository;
import md.mud.notificari.repository.app.AppMessageRepository;
import md.mud.notificari.service.messaging.Delivery;
import md.mud.notificari.service.messaging.SendOutcome;
import md.mud.notificari.service.messaging.VariableRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Citeste o livrare rezervata si scrie rezultatul ei. Ambele operatii stau in
 * tranzactii scurte, separate, ca intre ele sa incapa apelul de retea fara sa
 * tina o conexiune JDBC ocupata.
 *
 * Livrare "cel putin o data": daca predarea catre provider reuseste dar
 * {@link #complete} nu apuca sa faca commit (proces oprit, retea picata), randul
 * redevine vizibil dupa visibility-timeout si mesajul pleaca a doua oara. E
 * inerent oricarei cozi fara chei de idempotenta la provider - nu e un bug de
 * reparat aici.
 */
@Service
public class DeliveryExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(DeliveryExecutor.class);
    private static final int ERROR_MAX = 500;

    private final AppMessageRecipientRepository deliveries;
    private final AppMessageRepository messages;
    private final VariableRenderer renderer;
    private final RetryPolicy policy;

    public DeliveryExecutor(
        AppMessageRecipientRepository deliveries,
        AppMessageRepository messages,
        VariableRenderer renderer,
        RetryPolicy policy
    ) {
        this.deliveries = deliveries;
        this.messages = messages;
        this.renderer = renderer;
        this.policy = policy;
    }

    /**
     * Construieste sarcina de trimitere. Subiectul si corpul se re-randeaza acum
     * din starea persistata: substitutia e determinista, deci o reincercare da
     * acelasi rezultat, si nu avem nevoie de o coloana de payload.
     *
     * {@code attachmentCache} e detinut de apelant si golit la finalul fiecarui
     * tick: altfel un atasament de 10MB ar fi tinut in memorie o data pentru
     * fiecare destinatar al mesajului.
     *
     * Gol daca livrarea a disparut sau daca destinatarul nu mai are adresa pe canal.
     */
    @Transactional(readOnly = true)
    public Optional<DeliveryJob> load(Long deliveryId, Map<Long, List<Delivery.Attachment>> attachmentCache) {
        return deliveries
            .findForDispatch(deliveryId)
            .map(row -> {
                Message message = row.getMessage();
                Recipient recipient = row.getRecipient();
                Optional<String> address = addressFor(recipient, row.getChannel());
                if (address.isEmpty()) {
                    return null;
                }
                List<Delivery.Attachment> attachments = attachmentCache.computeIfAbsent(
                    message.getId(),
                    id -> attachmentsOf(message)
                );
                Delivery delivery = new Delivery(
                    address.get(),
                    fullName(recipient),
                    renderer.render(nullToEmpty(message.getSubject()), recipient),
                    renderer.render(nullToEmpty(message.getBodyHtml()), recipient),
                    attachments
                );
                return new DeliveryJob(
                    row.getId(),
                    message.getId(),
                    row.getChannel(),
                    row.getAttemptCount() == null ? 1 : row.getAttemptCount(),
                    delivery
                );
            });
    }

    /** Mesajul din care provine o livrare - pentru cazul in care nu s-a putut construi sarcina. */
    @Transactional(readOnly = true)
    public Optional<Long> messageIdOf(Long deliveryId) {
        return deliveries.findById(deliveryId).map(row -> row.getMessage().getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long deliveryId, SendOutcome outcome) {
        MessageRecipient row = deliveries.findById(deliveryId).orElse(null);
        if (row == null) {
            return;
        }
        Instant now = Instant.now();
        int attempt = row.getAttemptCount() == null ? 1 : row.getAttemptCount();
        switch (outcome.status()) {
            case OK -> row
                .status(DeliveryStatus.SENT)
                .providerMessageId(truncate(outcome.providerMessageId(), 120))
                .errorMessage(null)
                .sentAt(now)
                .nextAttemptAt(null);
            case PERMANENT_FAILURE -> row
                .status(DeliveryStatus.FAILED)
                .errorMessage(truncate(outcome.error(), ERROR_MAX))
                .nextAttemptAt(null);
            case RETRY -> {
                row.errorMessage(truncate(outcome.error(), ERROR_MAX));
                if (policy.exhausted(attempt)) {
                    row.status(DeliveryStatus.FAILED).nextAttemptAt(null);
                } else {
                    Duration delay = policy.delayFor(attempt, outcome.retryAfter());
                    row.status(DeliveryStatus.PENDING).nextAttemptAt(now.plus(delay));
                    LOG.debug("Livrarea {} se reia dupa {} (incercarea {})", deliveryId, delay, attempt);
                }
            }
        }
        deliveries.save(row);
    }

    /** Adresa a disparut intre punerea in coada si trimitere: nu e o eroare de provider. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markUnreachable(Long deliveryId) {
        deliveries
            .findById(deliveryId)
            .ifPresent(row ->
                deliveries.save(
                    row.status(DeliveryStatus.SKIPPED).errorMessage("Destinatarul nu mai are adresa activa pe acest canal.").nextAttemptAt(null)
                )
            );
    }

    private static List<Delivery.Attachment> attachmentsOf(Message message) {
        if (message.getAttachmentses() == null) {
            return List.of();
        }
        return message
            .getAttachmentses()
            .stream()
            .map(a -> new Delivery.Attachment(a.getFileName(), a.getContentType(), a.getFile()))
            .filter(a -> a.data() != null)
            .toList();
    }

    private static Optional<String> addressFor(Recipient r, Channel ch) {
        return r
            .getChannelses()
            .stream()
            .filter(c -> c.getChannel() == ch && Boolean.TRUE.equals(c.getActive()))
            .map(RecipientChannel::getAddress)
            .filter(a -> a != null && !a.isBlank())
            .findFirst();
    }

    private static String fullName(Recipient r) {
        return (nullToEmpty(r.getFirstName()) + " " + nullToEmpty(r.getLastName())).trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        return s == null ? null : s.substring(0, Math.min(s.length(), max));
    }
}
