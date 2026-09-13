package md.mud.notificari.service.messaging.telegram;

import java.util.List;
import md.mud.notificari.domain.enumeration.Channel;
import md.mud.notificari.service.messaging.ChannelSender;
import md.mud.notificari.service.messaging.Delivery;
import md.mud.notificari.service.messaging.SendOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Livrare reala prin Telegram Bot API.
 *
 * Ca la email, fara niciun fallback pe mock: daca nu vrei sa plece mesaje, pune
 * {@code application.messaging.channels.telegram.provider: mock}. Un fallback tacit
 * ar face istoricul ambiguu.
 *
 * Adresa canalului TELEGRAM e {@code chat_id}-ul numeric, nu un nume de utilizator:
 * Bot API nu accepta {@code @username} pentru persoane. Chat ID-urile se obtin din
 * ecranul de conectare (Destinatari -> Conecteaza Telegram), fiindca un bot nu poate
 * scrie primul nimanui.
 */
@Service
public class TelegramChannelSender implements ChannelSender {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramChannelSender.class);

    private final TelegramClient client;

    public TelegramChannelSender(TelegramClient client) {
        this.client = client;
    }

    @Override
    public Channel channel() {
        return Channel.TELEGRAM;
    }

    @Override
    public String providerId() {
        return "telegram-bot";
    }

    @Override
    public void validateConfiguration() {
        if (!client.hasToken()) {
            throw new IllegalStateException(
                "application.messaging.channels.telegram.provider=telegram-bot dar options.bot-token e gol. " +
                "Seteaza TELEGRAM_BOT_TOKEN (tokenul primit de la @BotFather) sau pune provider: mock."
            );
        }
        // Fara apel de retea aici, dinadins: o pana la Telegram nu trebuie sa impiedice pornirea aplicatiei.
    }

    @Override
    public SendOutcome send(Delivery delivery) {
        try {
            List<String> chunks = TelegramHtmlFormatter.chunks(source(delivery));
            if (chunks.isEmpty()) {
                // Nimic de trimis: mai bine un rand rosu explicit decat unul verde care nu inseamna nimic.
                return SendOutcome.permanent("Mesajul nu are text după reducerea pentru Telegram.");
            }
            Long firstMessageId = null;
            for (String chunk : chunks) {
                Long messageId = sendChunk(delivery.address(), chunk);
                if (firstMessageId == null) {
                    firstMessageId = messageId;
                }
            }
            // sendMessage nu poate purta fisiere, deci atasamentele pleaca dupa text.
            for (Delivery.Attachment attachment : delivery.attachments()) {
                client.sendDocument(delivery.address(), attachment);
            }
            return SendOutcome.ok(firstMessageId == null ? null : String.valueOf(firstMessageId));
        } catch (Exception e) {
            SendOutcome outcome = TelegramFailureClassifier.classify(e);
            LOG.warn("Telegram către {} a eșuat ({}): {}", delivery.address(), outcome.status(), outcome.error());
            return outcome;
        }
    }

    /**
     * Telegram nu are subiect (regula 8 din app.jdl), deci subiectul devine prima
     * linie - aceeasi conventie ca la mock. Se escapeaza, fiindca intra intr-o
     * sursa HTML.
     */
    private static String source(Delivery delivery) {
        String body = delivery.htmlBody() == null ? "" : delivery.htmlBody();
        String subject = delivery.subject();
        if (subject == null || subject.isBlank()) {
            return body;
        }
        return TelegramHtmlFormatter.escape(subject) + "<br><br>" + body;
    }

    /**
     * O bucata de mesaj. Daca Telegram refuza formatarea, o retrimitem o singura
     * data in text curat: un anunt e mai util fara ingrosari decat deloc. Warning-ul
     * spune ce tag a deranjat, ca formatter-ul sa poata fi completat.
     */
    private Long sendChunk(String chatId, String html) {
        try {
            return client.sendMessage(chatId, html, true);
        } catch (TelegramApiException e) {
            if (!TelegramFailureClassifier.formattingRejected(e)) {
                throw e;
            }
            LOG.warn("Telegram a respins formatarea ({}). Retrimit în text curat - de completat TelegramHtmlFormatter.", e.description());
            return client.sendMessage(chatId, TelegramHtmlFormatter.stripTags(html), false);
        }
    }
}
