package md.mud.notificari.service.app;

import md.mud.notificari.config.ApplicationProperties;
import md.mud.notificari.service.app.AppDtos.SendPayload;
import md.mud.notificari.service.app.AppDtos.SendResult;
import md.mud.notificari.service.messaging.dispatch.InlineDeliveryRunner;
import org.springframework.stereotype.Service;

/**
 * Punctul unic de intrare pentru trimitere, si locul unde se decide daca
 * mesajul pleaca asincron sau pe loc.
 *
 * Deliberat <b>fara</b> {@code @Transactional}: {@link AppService} e tranzactional
 * la nivel de clasa, deci nu poate face el dispecerizarea inline - ar readuce
 * apelul SMTP in tranzactie, iar un auto-apel nici nu ar fi interceptat de proxy.
 */
@Service
public class SendCoordinator {

    private final AppService app;
    private final InlineDeliveryRunner inlineRunner;
    private final ApplicationProperties properties;

    public SendCoordinator(AppService app, InlineDeliveryRunner inlineRunner, ApplicationProperties properties) {
        this.app = app;
        this.inlineRunner = inlineRunner;
        this.properties = properties;
    }

    public SendResult send(SendPayload payload) {
        SendResult queued = app.send(payload); // tranzactional, face commit aici
        if (properties.getMessaging().isAsync()) {
            return queued;
        }
        inlineRunner.drain(queued.messageId());
        return app.sendStatus(queued.messageId());
    }
}
