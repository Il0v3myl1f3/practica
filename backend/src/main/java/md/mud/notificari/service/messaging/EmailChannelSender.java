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

/** Real delivery over SMTP (Gmail in development). */
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
    public SendOutcome send(Delivery delivery) {
        if (username == null || username.isBlank()) {
            // Fara credentiale SMTP orice trimitere ar esua si tot istoricul ar fi rosu.
            // Pana se pun MAIL_USERNAME / MAIL_PASSWORD, emailul se comporta ca mock-urile.
            LOG.info("[EMAIL MOCK] catre {} ({}): {}", delivery.recipientName(), delivery.address(), delivery.subject());
            return SendOutcome.ok("mock-email-" + System.nanoTime());
        }
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
            LOG.warn("Email către {} a eșuat: {}", delivery.address(), e.getMessage());
            return SendOutcome.failed(e.getMessage());
        }
    }
}
