package md.mud.notificari.service.messaging.telegram;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
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
 * Clientul HTTP pentru Bot API. Subtire cu bunastiinta: nu stie nimic despre
 * livrari, destinatari sau formatare - doar cheama metode si traduce raspunsul.
 *
 * Bean-ul exista mereu, chiar si cand canalul e pe mock: nu deschide nicio
 * conexiune pana nu e chemat.
 */
@Component
public class TelegramClient {

    /** Limita Telegram pentru un mesaj trimis cu sendMessage. */
    public static final int MESSAGE_LIMIT = 4096;

    private static final String DEFAULT_BASE_URL = "https://api.telegram.org";

    private final RestClient http;
    private final ObjectMapper json;
    private final String token;
    private final String baseUrl;

    public TelegramClient(ObjectMapper json, ApplicationProperties properties) {
        Map<String, String> options = properties.getMessaging().settingsFor(Channel.TELEGRAM).getOptions();
        this.token = options.getOrDefault("bot-token", "").trim();
        this.baseUrl = trimTrailingSlash(options.getOrDefault("base-url", DEFAULT_BASE_URL));
        this.json = json;
        // RestClient construit de mana: in Spring Boot 4 autoconfigurarea lui
        // RestClient.Builder sta intr-un modul care nu e pe classpath-ul proiectului.
        this.http = RestClient.builder().requestFactory(requestFactory()).build();
    }

    /**
     * Timeout-uri explicite, aceeasi lectie ca la SMTP: dispecerul ruleaza pe
     * planificatorul de task-uri, iar un socket blocat l-ar opri cu totul.
     *
     * Fabrica se construieste de mana, nu cu ajutoarele din Spring Boot:
     * {@code spring-boot-http-client} nu e pe classpath-ul acestui proiect.
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

    /**
     * @param html false = text curat, fara parse_mode (calea de rezerva cand
     *             Telegram refuza formatarea)
     * @return message_id-ul mesajului trimis
     */
    public Long sendMessage(String chatId, String text, boolean html) {
        Map<String, Object> body = html
            ? Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML")
            : Map.of("chat_id", chatId, "text", text);
        TelegramResponse response = post("sendMessage", body, MediaType.APPLICATION_JSON, TelegramResponse.class);
        return response.result() == null ? null : response.result().messageId();
    }

    /** sendMessage nu poate purta fisiere, deci atasamentele pleaca separat. */
    public Long sendDocument(String chatId, Delivery.Attachment attachment) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("chat_id", chatId);
        form.add("document", filePart(attachment));
        TelegramResponse response = post("sendDocument", form, MediaType.MULTIPART_FORM_DATA, TelegramResponse.class);
        return response.result() == null ? null : response.result().messageId();
    }

    /** Numele botului, pentru linkul t.me/... pe care il dai oamenilor. */
    public String botUsername() {
        TelegramResponse response = post("getMe", Map.of(), MediaType.APPLICATION_JSON, TelegramResponse.class);
        return response.result() == null ? null : response.result().username();
    }

    /**
     * Mesajele primite de bot.
     *
     * <b>Fara offset, deliberat.</b> Un offset confirma update-urile si Telegram
     * nu le mai trimite niciodata - lista de contacte ar fi goala de a doua
     * deschidere a ecranului. Asa, acelasi apel poate fi facut de oricate ori.
     */
    public List<TelegramUpdates.Update> updates() {
        TelegramUpdates response = post(
            "getUpdates",
            Map.of("timeout", 0, "limit", 100, "allowed_updates", List.of("message")),
            MediaType.APPLICATION_JSON,
            TelegramUpdates.class
        );
        return response.result() == null ? List.of() : response.result();
    }

    private <T> T post(String method, Object body, MediaType contentType, Class<T> type) {
        // Corpurile JSON se serializeaza aici, nu de un convertor de mesaje: la fel
        // ca RestClient.Builder, convertorul Jackson vine din autoconfigurare.
        Object payload = MediaType.APPLICATION_JSON.equals(contentType) ? json.writeValueAsString(body) : body;
        // URI complet, nu template: tokenul contine ':' si expandarea de variabile
        // l-ar codifica in %3A, iar Telegram ar raspunde 404.
        ResponseEntity<String> raw = http
            .post()
            .uri(URI.create(baseUrl + "/bot" + token + "/" + method))
            .contentType(contentType)
            .body(payload)
            .retrieve()
            // Tratarea implicita a erorilor ar arunca inainte sa citim corpul, iar
            // motivul adevarat ("chat not found") e chiar in corp.
            .onStatus(status -> true, (request, response) -> {})
            .toEntity(String.class);

        T parsed = parse(raw.getBody(), type);
        if (raw.getStatusCode().is2xxSuccessful() && ok(parsed)) {
            return parsed;
        }
        throw new TelegramApiException(raw.getStatusCode().value(), errorCode(parsed), description(parsed, raw.getBody()), retryAfter(parsed));
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

    private static boolean ok(Object parsed) {
        return switch (parsed) {
            case TelegramResponse r -> r.ok();
            case TelegramUpdates u -> u.ok();
            case null, default -> false;
        };
    }

    private static Integer errorCode(Object parsed) {
        return switch (parsed) {
            case TelegramResponse r -> r.errorCode();
            case TelegramUpdates u -> u.errorCode();
            case null, default -> null;
        };
    }

    private static String description(Object parsed, String rawBody) {
        String description = switch (parsed) {
            case TelegramResponse r -> r.description();
            case TelegramUpdates u -> u.description();
            case null, default -> null;
        };
        if (description != null && !description.isBlank()) {
            return description;
        }
        // Nu e JSON de la Telegram: pastram un fragment din corp, ca sa se vada in istoric cine a raspuns.
        return rawBody == null ? "" : rawBody.substring(0, Math.min(rawBody.length(), 200)).strip();
    }

    private static Integer retryAfter(Object parsed) {
        TelegramResponse.Parameters parameters = switch (parsed) {
            case TelegramResponse r -> r.parameters();
            case TelegramUpdates u -> u.parameters();
            case null, default -> null;
        };
        return parameters == null ? null : parameters.retryAfter();
    }

    private static String trimTrailingSlash(String url) {
        String trimmed = url == null ? DEFAULT_BASE_URL : url.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_BASE_URL;
        }
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
