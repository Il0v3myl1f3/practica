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

    public static RecipientException invalidDiscordId() {
        return new RecipientException(HttpStatus.BAD_REQUEST, "User ID-ul Discord nu e valid (id numeric de 17-20 cifre).", "invaliddiscordid");
    }

    /** Acelasi user Discord nu poate fi al doua persoane: mesajele ar ajunge de doua ori la unul. */
    public static RecipientException discordIdAlreadyLinked(String recipientName) {
        return new RecipientException(HttpStatus.CONFLICT, "Acest user Discord e deja legat la " + recipientName + ".", "discordidexists");
    }

    /** Canalul DISCORD e pe alt provider (de obicei mock): nu avem de unde citi membrii serverului. */
    public static RecipientException discordNotActive(String activeProvider) {
        return new RecipientException(
            HttpStatus.CONFLICT,
            "Discordul real nu e pornit (provider activ: " +
            activeProvider +
            "). Seteaza MESSAGING_DISCORD_PROVIDER=discord-bot si DISCORD_BOT_TOKEN.",
            "discordnotactive"
        );
    }

    /** Provider-ul e discord-bot, dar lipseste id-ul serverului. */
    public static RecipientException discordGuildMissing() {
        return new RecipientException(HttpStatus.CONFLICT, "Discordul real nu e pornit: lipseste DISCORD_GUILD_ID.", "discordnotactive");
    }

    /** Discord a raspuns cu o eroare la citirea membrilor serverului. */
    public static RecipientException discordUnavailable(String reason) {
        return new RecipientException(HttpStatus.BAD_GATEWAY, "Discord nu a raspuns: " + reason, "discordunavailable");
    }
}
