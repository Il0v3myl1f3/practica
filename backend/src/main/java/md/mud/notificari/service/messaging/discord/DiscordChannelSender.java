package md.mud.notificari.service.messaging.discord;

import java.util.List;
import md.mud.notificari.domain.enumeration.Channel;
import md.mud.notificari.service.messaging.ChannelSender;
import md.mud.notificari.service.messaging.Delivery;
import md.mud.notificari.service.messaging.SendOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Livrare reala prin Discord: un mesaj privat (DM) catre destinatar.
 *
 * Ca la Telegram, fara niciun fallback pe mock: daca nu vrei sa plece mesaje,
 * pune {@code application.messaging.channels.discord.provider: mock}.
 *
 * Adresa canalului DISCORD e user id-ul numeric (snowflake), nu un nume de
 * utilizator. Se obtine din ecranul de conectare (Destinatari -> Conecteaza
 * Discord) sau scris manual. Botul poate trimite DM doar cuiva de pe acelasi
 * server si care nu a dezactivat DM-urile de la membrii serverului.
 */
@Service
public class DiscordChannelSender implements ChannelSender {

    private static final Logger LOG = LoggerFactory.getLogger(DiscordChannelSender.class);

    private final DiscordClient client;

    public DiscordChannelSender(DiscordClient client) {
        this.client = client;
    }

    @Override
    public Channel channel() {
        return Channel.DISCORD;
    }

    @Override
    public String providerId() {
        return "discord-bot";
    }

    @Override
    public void validateConfiguration() {
        if (!client.hasToken()) {
            throw new IllegalStateException(
                "application.messaging.channels.discord.provider=discord-bot dar options.bot-token e gol. " +
                "Seteaza DISCORD_BOT_TOKEN (tokenul botului din Developer Portal) sau pune provider: mock."
            );
        }
        // Fara apel de retea aici, dinadins: o pana la Discord nu trebuie sa impiedice pornirea aplicatiei.
    }

    @Override
    public SendOutcome send(Delivery delivery) {
        try {
            List<String> chunks = DiscordMarkdownFormatter.chunks(source(delivery));
            if (chunks.isEmpty()) {
                // Nimic de trimis: mai bine un rand rosu explicit decat unul verde care nu inseamna nimic.
                return SendOutcome.permanent("Mesajul nu are text după reducerea pentru Discord.");
            }
            String dmId = client.openDm(delivery.address());
            String firstMessageId = null;
            for (String chunk : chunks) {
                String messageId = client.sendMessage(dmId, chunk);
                if (firstMessageId == null) {
                    firstMessageId = messageId;
                }
            }
            // sendMessage nu poate purta fisiere, deci atasamentele pleaca dupa text, in loturi de maxim 10.
            List<Delivery.Attachment> attachments = delivery.attachments();
            for (int i = 0; i < attachments.size(); i += DiscordClient.MAX_FILES_PER_MESSAGE) {
                client.sendFiles(dmId, attachments.subList(i, Math.min(i + DiscordClient.MAX_FILES_PER_MESSAGE, attachments.size())));
            }
            return SendOutcome.ok(firstMessageId);
        } catch (Exception e) {
            SendOutcome outcome = DiscordFailureClassifier.classify(e);
            LOG.warn("Discord către {} a eșuat ({}): {}", delivery.address(), outcome.status(), outcome.error());
            return outcome;
        }
    }

    /**
     * Discord nu are subiect (regula 8 din app.jdl), deci subiectul devine prima
     * linie, ingrosata - aceeasi conventie ca la Telegram, doar ca aici rezultatul
     * e {@code **subiect**}. Se ambaleaza in {@code <b>}, nu direct in Markdown:
     * sursa intra tot prin conversia HTML -> Markdown, ca restul corpului, si
     * asa nu se ciocneste cu escaparea marcajelor din text.
     */
    private static String source(Delivery delivery) {
        String body = delivery.htmlBody() == null ? "" : delivery.htmlBody();
        String subject = delivery.subject();
        if (subject == null || subject.isBlank()) {
            return body;
        }
        return "<b>" + escapeHtml(subject) + "</b><br><br>" + body;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
