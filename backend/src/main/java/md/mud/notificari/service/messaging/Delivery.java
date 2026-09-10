package md.mud.notificari.service.messaging;

import java.util.List;

/** One rendered message, ready to go out to one recipient on one channel. */
public record Delivery(String address, String recipientName, String subject, String htmlBody, List<Attachment> attachments) {
    public record Attachment(String fileName, String contentType, byte[] data) {}
}
