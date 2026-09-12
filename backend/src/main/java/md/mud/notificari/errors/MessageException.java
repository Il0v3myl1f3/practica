package md.mud.notificari.errors;

import org.springframework.http.HttpStatus;

public final class MessageException extends EntityErrorException {

    private static final String ENTITY_NAME = "message";

    private MessageException(HttpStatus status, String defaultMessage, String errorKey) {
        super(status, defaultMessage, ENTITY_NAME, errorKey);
    }

    public static MessageException notFound(Long id) {
        return new MessageException(HttpStatus.NOT_FOUND, "Mesajul " + id + " nu a fost gasit.", "notfound");
    }

    public static MessageException noChannelSelected() {
        return new MessageException(HttpStatus.BAD_REQUEST, "Alege cel putin un canal.", "nochannel");
    }

    public static MessageException noRecipientSelected() {
        return new MessageException(HttpStatus.BAD_REQUEST, "Bifeaza cel putin un destinatar.", "norecipient");
    }
}
