package md.mud.notificari.errors;

import org.springframework.http.HttpStatus;

public final class RecipientGroupException extends EntityErrorException {

    private static final String ENTITY_NAME = "recipientGroup";

    private RecipientGroupException(HttpStatus status, String defaultMessage, String errorKey) {
        super(status, defaultMessage, ENTITY_NAME, errorKey);
    }

    public static RecipientGroupException notFound(Long id) {
        return new RecipientGroupException(HttpStatus.NOT_FOUND, "Recipient group " + id + " was not found", "notfound");
    }
}
