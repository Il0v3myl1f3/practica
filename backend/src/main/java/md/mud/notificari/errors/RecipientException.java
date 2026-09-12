package md.mud.notificari.errors;

import org.springframework.http.HttpStatus;

public final class RecipientException extends EntityErrorException {

    private static final String ENTITY_NAME = "recipient";

    private RecipientException(HttpStatus status, String defaultMessage, String errorKey) {
        super(status, defaultMessage, ENTITY_NAME, errorKey);
    }

    public static RecipientException notFound(Long id) {
        return new RecipientException(HttpStatus.NOT_FOUND, "Destinatarul " + id + " nu a fost gasit.", "notfound");
    }

    public static RecipientException emailAlreadyUsed() {
        return new RecipientException(HttpStatus.BAD_REQUEST, "Un alt destinatar are deja acest email.", "emailexists");
    }
}
