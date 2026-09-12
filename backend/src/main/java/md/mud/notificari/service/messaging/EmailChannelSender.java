package md.mud.notificari.service.messaging;

import jakarta.mail.internet.MimeMessage;
import md.mud.notificari.domain.enumeration.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Livrare reala prin SMTP (Gmail in dezvoltare).
 *
 * Nu are niciun fallback pe mock: daca nu vrei sa plece emailuri, pune
 * {@code application.messaging.channels.email.provider: mock}. Un fallback tacit
 * ar face istoricul ambiguu - nu ai mai sti daca un rand verde a fost real.
 */
@Service
public class EmailChannelSender implements ChannelSender {

    private static final Logger LOG = LoggerFactory.getLogger(EmailChannelSender.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String username;

    public EmailChannelSender(
        JavaMailSender mailSender,
        @Value("${jhipster.mail.from:no-reply@notificari-mud.md}") String from,
        @Value("${spring.mail.username:}") String username
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.username = username;
    }

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public String providerId() {
        return "smtp";
    }

    @Override
    public void validateConfiguration() {
        if (username == null || username.isBlank()) {
            throw new IllegalStateException(
                "application.messaging.channels.email.provider=smtp dar spring.mail.username e gol. " +
                "Seteaza MAIL_USERNAME si MAIL_PASSWORD (App password din contul Google, nu parola contului) " +
                "sau pune provider: mock."
            );
        }
    }

    @Override
    public SendOutcome send(Delivery delivery) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, !delivery.attachments().isEmpty(), "UTF-8");
            helper.setFrom(from);
            helper.setTo(delivery.address());
            helper.setSubject(delivery.subject());
            helper.setText(delivery.htmlBody(), true);
            for (Delivery.Attachment a : delivery.attachments()) {
                helper.addAttachment(a.fileName(), new ByteArrayResource(a.data()), a.contentType());
            }
            mailSender.send(mime);
            return SendOutcome.ok(mime.getMessageID());
        } catch (Exception e) {
            SendOutcome outcome = EmailFailureClassifier.classify(e);
            LOG.warn("Email către {} a eșuat ({}): {}", delivery.address(), outcome.status(), outcome.error());
            return outcome;
        }
    }
}
