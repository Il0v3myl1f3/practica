package md.mud.notificari.service.messaging.discord;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import md.mud.notificari.config.ApplicationProperties;
import md.mud.notificari.domain.enumeration.Channel;
import md.mud.notificari.service.messaging.Delivery;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Clientul HTTP pentru Discord API v10. Subtire cu bunastiinta, ca
 * {@code TelegramClient}: nu stie nimic despre livrari sau formatare - doar
 * cheama metode si traduce raspunsul.
 *
 * Bean-ul exista mereu, chiar si cand canalul e pe mock: nu deschide nicio
 * conexiune pana nu e chemat.
 *
 * Diferenta fata de Telegram: Discord foloseste verbe HTTP reale (GET pentru
 * citiri) si nu invaluie raspunsul intr-un plic cu {@code ok} - reusita sau
 * esecul se afla direct din codul HTTP.
 */
@Component
public class DiscordClient {

    /** Limita Discord pentru continutul unui mesaj. */
    public static final int MESSAGE_LIMIT = 2000;

    /** Fisiere pe mesaj, cate poate purta un singur apel sendFiles. */
    public static final int MAX_FILES_PER_MESSAGE = 10;

    private static final String DEFAULT_BASE_URL = "https://discord.com/api/v10";

    // Fara User-Agent, Cloudflare poate bloca cererea inainte sa ajunga la Discord.
    private static final String USER_AGENT = "DiscordBot (https://github.com/notificari-mud, 1.0)";

    private final RestClient http;
    private final ObjectMapper json;
    private final String token;
    private final String guildId;
    private final String baseUrl;

    public DiscordClient(ObjectMapper json, ApplicationProperties properties) {
        Map<String, String> options = properties.getMessaging().settingsFor(Channel.DISCORD).getOptions();
        this.token = options.getOrDefault("bot-token", "").trim();
        this.guildId = options.getOrDefault("guild-id", "").trim();
        this.baseUrl = trimTrailingSlash(options.getOrDefault("base-url", DEFAULT_BASE_URL));
        this.json = json;
        // RestClient construit de mana: in Spring Boot 4 autoconfigurarea lui
        // RestClient.Builder sta intr-un modul care nu e pe classpath-ul proiectului.
        this.http = RestClient.builder().requestFactory(requestFactory()).build();
    }

    /**
     * Timeout-uri explicite, aceeasi lectie ca la Telegram: dispecerul ruleaza pe
     * planificatorul de task-uri, iar un socket blocat l-ar opri cu totul.
     */
    private static ClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
        );
        factory.setReadTimeout(Duration.ofSeconds(20));
        return factory;
    }

    public boolean hasToken() {
        return !token.isBlank();
    }

    public String guildId() {
        return guildId;
    }

    /** Deschide (idempotent) canalul DM cu un utilizator si intoarce id-ul lui. */
    public String openDm(String userId) {
        DiscordResponses.Channel channel = post(
            "/users/@me/channels",
            Map.of("recipient_id", userId),
            MediaType.APPLICATION_JSON,
            DiscordResponses.Channel.class
        );
        return channel == null ? null : channel.id();
    }

    /** @return id-ul mesajului trimis */
    public String sendMessage(String channelId, String content) {
        Map<String, Object> body = Map.of("content", content, "allowed_mentions", Map.of("parse", List.of()));
        DiscordResponses.Message response = post("/channels/" + channelId + "/messages", body, MediaType.APPLICATION_JSON, DiscordResponses.Message.class);
        return response == null ? null : response.id();
    }

    /**
     * sendMessage nu poate purta fisiere, deci atasamentele pleaca separat, cu un
     * corp multipart: {@code payload_json} cu metadatele si {@code files[n]} cu
     * continutul. Fara metadatele din {@code payload_json}, Discord ignora fisierele.
     */
    public String sendFiles(String channelId, List<Delivery.Attachment> attachments) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        List<Map<String, Object>> meta = new ArrayList<>();
        for (int i = 0; i < attachments.size(); i++) {
            meta.add(Map.of("id", i, "filename", attachments.get(i).fileName()));
        }
        form.add("payload_json", json.writeValueAsString(Map.of("attachments", meta, "allowed_mentions", Map.of("parse", List.of()))));
        for (int i = 0; i < attachments.size(); i++) {
            form.add("files[" + i + "]", filePart(attachments.get(i)));
        }
        DiscordResponses.Message response = post(
            "/channels/" + channelId + "/messages",
            form,
            MediaType.MULTIPART_FORM_DATA,
            DiscordResponses.Message.class
        );
        return response == null ? null : response.id();
    }

    /** Botul insusi - id-ul e folosit pentru linkul de invitare. */
    public DiscordResponses.User botUser() {
        return get("/users/@me", DiscordResponses.User.class);
    }

    /** Numele serverului, pentru ecranul de conectare. */
    public String guildName() {
        DiscordResponses.Guild guild = get("/guilds/" + guildId, DiscordResponses.Guild.class);
        return guild == null ? null : guild.name();
    }

    /**
     * Membrii serverului, fara boti. Cere Server Members Intent bifat in
     * Developer Portal, altfel Discord raspunde cu o lista goala sau cu eroare.
     *
     * Paginare cu {@code after=<ultimul id>}: continuam cat timp pagina anterioara
     * a venit plina (1000).
     */
    public List<DiscordResponses.Member> members() {
        List<DiscordResponses.Member> all = new ArrayList<>();
        String after = "0";
        while (true) {
            DiscordResponses.Member[] page = get("/guilds/" + guildId + "/members?limit=1000&after=" + after, DiscordResponses.Member[].class);
            if (page == null || page.length == 0) {
                break;
            }
            for (DiscordResponses.Member member : page) {
                if (member.user() != null && !Boolean.TRUE.equals(member.user().bot())) {
                    all.add(member);
                }
            }
            if (page.length < 1000) {
                break;
            }
            after = page[page.length - 1].user().id();
        }
        return all;
    }

    private <T> T get(String path, Class<T> type) {
        ResponseEntity<String> raw = http
            .get()
            .uri(URI.create(baseUrl + path))
            .header(HttpHeaders.AUTHORIZATION, "Bot " + token)
            .header(HttpHeaders.USER_AGENT, USER_AGENT)
            .retrieve()
            // Tratarea implicita a erorilor ar arunca inainte sa citim corpul, iar
            // motivul adevarat ("Unknown Guild"...) e chiar in corp.
            .onStatus(status -> true, (request, response) -> {})
            .toEntity(String.class);
        return finish(raw, type);
    }

    private <T> T post(String path, Object body, MediaType contentType, Class<T> type) {
        // Corpurile JSON se serializeaza aici, nu de un convertor de mesaje: la fel
        // ca RestClient.Builder, convertorul Jackson vine din autoconfigurare.
        Object payload = MediaType.APPLICATION_JSON.equals(contentType) ? json.writeValueAsString(body) : body;
        ResponseEntity<String> raw = http
            .post()
            .uri(URI.create(baseUrl + path))
            .header(HttpHeaders.AUTHORIZATION, "Bot " + token)
            .header(HttpHeaders.USER_AGENT, USER_AGENT)
            .contentType(contentType)
            .body(payload)
            .retrieve()
            .onStatus(status -> true, (request, response) -> {})
            .toEntity(String.class);
        return finish(raw, type);
    }

    /**
     * Discord nu invaluie raspunsul intr-un plic cu {@code ok}, ca Telegram:
     * reusita se vede direct din codul HTTP. La non-2xx corpul e {@code {code,
     * message, retry_after?}}.
     */
    private <T> T finish(ResponseEntity<String> raw, Class<T> type) {
        if (raw.getStatusCode().is2xxSuccessful()) {
            // 204 sau alt corp gol la succes: nimic de parsat, nimic de aruncat.
            return parse(raw.getBody(), type);
        }
        DiscordResponses.Error error = parse(raw.getBody(), DiscordResponses.Error.class);
        Integer code = error == null ? null : error.code();
        String message = error != null && error.message() != null && !error.message().isBlank() ? error.message() : rawBody(raw.getBody());
        Duration retryAfter = error == null ? null : DiscordApiException.roundUp(error.retryAfter());
        throw new DiscordApiException(raw.getStatusCode().value(), code, message, retryAfter);
    }

    /** Un proxy sau un gateway poate raspunde cu HTML; atunci nu avem ce parsa. */
    private <T> T parse(String body, Class<T> type) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return json.readValue(body, type);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Nu e JSON de la Discord: pastram un fragment din corp, ca sa se vada in istoric cine a raspuns. */
    private static String rawBody(String body) {
        return body == null ? "" : body.substring(0, Math.min(body.length(), 200)).strip();
    }

    private static HttpEntity<ByteArrayResource> filePart(Delivery.Attachment attachment) {
        ByteArrayResource resource = new ByteArrayResource(attachment.data()) {
            @Override
            public String getFilename() {
                return attachment.fileName();
            }
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType(attachment.contentType()));
        return new HttpEntity<>(resource, headers);
    }

    private static MediaType mediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static String trimTrailingSlash(String url) {
        String trimmed = url == null ? DEFAULT_BASE_URL : url.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_BASE_URL;
        }
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
