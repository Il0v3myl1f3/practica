package md.mud.notificari.errors;

import org.springframework.http.HttpStatus;

public final class GroupException extends EntityErrorException {

    private static final String ENTITY_NAME = "group";

    private GroupException(HttpStatus status, String defaultMessage, String errorKey) {
        super(status, defaultMessage, ENTITY_NAME, errorKey);
    }

    public static GroupException notFound(Long id) {
        return new GroupException(HttpStatus.NOT_FOUND, "Grupul " + id + " nu a fost gasit.", "notfound");
    }

    public static GroupException nameAlreadyUsed() {
        return new GroupException(HttpStatus.BAD_REQUEST, "Un alt grup are deja acest nume.", "nameexists");
    }

    /** FK-ul de pe Recipient e obligatoriu: un grup cu destinatari nu poate fi sters fara sa-i mute intai. */
    public static GroupException hasRecipients(long count) {
        return new GroupException(
            HttpStatus.CONFLICT,
            "Grupul are " + count + " " + (count == 1 ? "destinatar" : "destinatari") + " - muta-i intai in alt grup.",
            "hasrecipients"
        );
    }
}
