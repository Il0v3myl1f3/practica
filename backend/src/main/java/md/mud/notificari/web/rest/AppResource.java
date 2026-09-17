package md.mud.notificari.web.rest;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import md.mud.notificari.service.app.AppDtos.ComposePayload;
import md.mud.notificari.service.app.AppDtos.DiscordDirectory;
import md.mud.notificari.service.app.AppDtos.DiscordLink;
import md.mud.notificari.service.app.AppDtos.GroupView;
import md.mud.notificari.service.app.AppDtos.MessageDetail;
import md.mud.notificari.service.app.AppDtos.MessageView;
import md.mud.notificari.service.app.AppDtos.OverviewView;
import md.mud.notificari.service.app.AppDtos.RecipientUpsert;
import md.mud.notificari.service.app.AppDtos.RecipientView;
import md.mud.notificari.service.app.AppDtos.SendPayload;
import md.mud.notificari.service.app.AppDtos.SendResult;
import md.mud.notificari.service.app.AppDtos.TelegramDirectory;
import md.mud.notificari.service.app.AppDtos.TelegramLink;
import md.mud.notificari.service.app.AppDtos.TemplateView;
import md.mud.notificari.service.app.AppService;
import md.mud.notificari.service.app.DiscordLinkService;
import md.mud.notificari.service.app.SendCoordinator;
import md.mud.notificari.service.app.TelegramLinkService;
import md.mud.notificari.service.app.TemplateArchiveService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The API the Angular client talks to. One endpoint per thing a screen needs,
 * everything already scoped to the organization of the signed-in user.
 */
@RestController
@RequestMapping("/api/app")
public class AppResource {

    private final AppService app;
    private final SendCoordinator sending;
    private final TemplateArchiveService archive;
    private final TelegramLinkService telegram;
    private final DiscordLinkService discord;

    public AppResource(
        AppService app,
        SendCoordinator sending,
        TemplateArchiveService archive,
        TelegramLinkService telegram,
        DiscordLinkService discord
    ) {
        this.app = app;
        this.sending = sending;
        this.archive = archive;
        this.telegram = telegram;
        this.discord = discord;
    }

    // ------------------------------------------------------------- dashboard

    @GetMapping("/overview")
    public OverviewView overview() {
        return app.overview();
    }

    // ------------------------------------------------------------ recipients

    @GetMapping("/recipients")
    public List<RecipientView> recipients() {
        return app.recipients();
    }

    @PostMapping("/recipients")
    public RecipientView createRecipient(@RequestBody RecipientUpsert body) {
        return app.createRecipient(body);
    }

    @PutMapping("/recipients/{id}")
    public RecipientView updateRecipient(@PathVariable Long id, @RequestBody RecipientUpsert body) {
        return app.updateRecipient(id, body);
    }

    @DeleteMapping("/recipients/{id}")
    public ResponseEntity<Void> deleteRecipient(@PathVariable Long id) {
        app.deleteRecipient(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recipients/bulk")
    public List<RecipientView> importRecipients(@RequestBody List<RecipientUpsert> body) {
        return app.importRecipients(body);
    }

    @GetMapping("/groups")
    public List<GroupView> groups() {
        return app.groups();
    }

    // -------------------------------------------------------------- telegram

    /** Oamenii care au scris botului, cu chat ID-ul lor - de aici se leaga destinatarii. */
    @GetMapping("/telegram/contacts")
    public TelegramDirectory telegramContacts() {
        return telegram.directory();
    }

    @PostMapping("/telegram/link")
    public ResponseEntity<Void> linkTelegram(@RequestBody TelegramLink body) {
        telegram.link(body);
        return ResponseEntity.noContent().build();
    }

    // --------------------------------------------------------------- discord

    /** Membrii serverului, cu user id-ul lor - de aici se leaga destinatarii. */
    @GetMapping("/discord/members")
    public DiscordDirectory discordMembers() {
        return discord.directory();
    }

    @PostMapping("/discord/link")
    public ResponseEntity<Void> linkDiscord(@RequestBody DiscordLink body) {
        discord.link(body);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------- templates

    @GetMapping("/templates")
    public List<TemplateView> templates() {
        return app.templates();
    }

    @PostMapping("/templates")
    public TemplateView createTemplate(@RequestBody TemplateView body) {
        return app.createTemplate(body);
    }

    @PutMapping("/templates/{id}")
    public TemplateView updateTemplate(@PathVariable Long id, @RequestBody TemplateView body) {
        return app.updateTemplate(id, body);
    }

    /** Toate sabloanele, ca arhiva ZIP cu cate un fisier HTML fiecare. */
    @GetMapping(value = "/templates/export", produces = "application/zip")
    public ResponseEntity<byte[]> exportTemplates() {
        byte[] zip = archive.export(app.templates());
        String name = "sabloane-" + LocalDate.now() + ".zip";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
            .body(zip);
    }

    @PostMapping(value = "/templates/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<TemplateView> importTemplates(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Nu ai ales niciun fisier.");
        }
        try (InputStream in = file.getInputStream()) {
            return app.importTemplates(archive.read(in));
        } catch (IOException e) {
            throw new IllegalArgumentException("Fisierul nu a putut fi citit.");
        }
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        app.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------- messages

    @GetMapping("/messages")
    public List<MessageView> sent() {
        return app.sentMessages();
    }

    @GetMapping("/messages/{id}")
    public MessageDetail message(@PathVariable Long id) {
        return app.message(id);
    }

    @PostMapping("/messages/send")
    public SendResult send(@RequestBody SendPayload body) {
        return sending.send(body);
    }

    /** Starea trimiterii, pentru ecranul de compunere cat timp mesajul e in coada. */
    @GetMapping("/messages/{id}/status")
    public SendResult sendStatus(@PathVariable Long id) {
        return app.sendStatus(id);
    }

    @GetMapping("/drafts")
    public List<MessageView> drafts() {
        return app.drafts();
    }

    @PostMapping("/drafts")
    public MessageView saveDraft(@RequestBody ComposePayload body) {
        return app.saveDraft(body);
    }

    @DeleteMapping("/drafts/{id}")
    public ResponseEntity<Void> deleteDraft(@PathVariable Long id) {
        app.deleteDraft(id);
        return ResponseEntity.noContent().build();
    }

    /** Validation failures carry a message the UI shows verbatim. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }
}
