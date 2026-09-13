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

    public static RecipientException invalidChatId() {
        return new RecipientException(HttpStatus.BAD_REQUEST, "Chat ID-ul Telegram lipseste.", "invalidchatid");
    }

    /** Acelasi chat Telegram nu poate fi al doua persoane: mesajele ar ajunge de doua ori la unul. */
    public static RecipientException chatIdAlreadyLinked(String recipientName) {
        return new RecipientException(HttpStatus.CONFLICT, "Acest chat Telegram e deja legat la " + recipientName + ".", "chatidexists");
    }

    /** Canalul TELEGRAM e pe alt provider (de obicei mock): nu avem de unde citi contactele botului. */
    public static RecipientException telegramNotActive(String activeProvider) {
        return new RecipientException(
            HttpStatus.CONFLICT,
            "Telegramul real nu e pornit (provider activ: " +
            activeProvider +
            "). Seteaza MESSAGING_TELEGRAM_PROVIDER=telegram-bot si TELEGRAM_BOT_TOKEN.",
            "telegramnotactive"
        );
    }

    /** Telegram a raspuns cu o eroare la citirea contactelor. */
    public static RecipientException telegramUnavailable(String reason) {
        return new RecipientException(HttpStatus.BAD_GATEWAY, "Telegram nu a raspuns: " + reason, "telegramunavailable");
    }
}
