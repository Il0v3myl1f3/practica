package md.mud.notificari.errors;

import org.springframework.http.HttpStatus;

public final class MessageTemplateException extends EntityErrorException {

    private static final String ENTITY_NAME = "messageTemplate";

    private MessageTemplateException(HttpStatus status, String defaultMessage, String errorKey) {
        super(status, defaultMessage, ENTITY_NAME, errorKey);
    }

    public static MessageTemplateException notFound(Long id) {
        return new MessageTemplateException(HttpStatus.NOT_FOUND, "Sablonul " + id + " nu a fost gasit.", "notfound");
    }

    public static MessageTemplateException nameAlreadyUsed() {
        return new MessageTemplateException(HttpStatus.BAD_REQUEST, "Exista deja un sablon cu acest nume.", "nameexists");
    }

    public static MessageTemplateException invalidArchive() {
        return new MessageTemplateException(HttpStatus.BAD_REQUEST, "Fisierul nu este o arhiva ZIP valida.", "invalidarchive");
    }

    public static MessageTemplateException emptyArchive() {
        return new MessageTemplateException(HttpStatus.BAD_REQUEST, "Arhiva nu contine niciun fisier .html.", "emptyarchive");
    }
}
