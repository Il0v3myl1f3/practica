package md.mud.notificari.web.rest;

import java.util.List;
import java.util.Map;
import md.mud.notificari.service.app.AppDtos.ComposePayload;
import md.mud.notificari.service.app.AppDtos.GroupView;
import md.mud.notificari.service.app.AppDtos.MessageDetail;
import md.mud.notificari.service.app.AppDtos.MessageView;
import md.mud.notificari.service.app.AppDtos.OverviewView;
import md.mud.notificari.service.app.AppDtos.RecipientUpsert;
import md.mud.notificari.service.app.AppDtos.RecipientView;
import md.mud.notificari.service.app.AppDtos.SendPayload;
import md.mud.notificari.service.app.AppDtos.SendResult;
import md.mud.notificari.service.app.AppDtos.TemplateView;
import md.mud.notificari.service.app.AppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The API the Angular client talks to. One endpoint per thing a screen needs,
 * everything already scoped to the organization of the signed-in user.
 */
@RestController
@RequestMapping("/api/app")
public class AppResource {

    private final AppService app;

    public AppResource(AppService app) {
        this.app = app;
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

    // ------------------------------------------------------------- templates

    @GetMapping("/templates")
    public List<TemplateView> templates() {
        return app.templates();
    }

    @PostMapping("/templates")
    public TemplateView createTemplate(@RequestBody TemplateView body) {
        return app.createTemplate(body);
    }

    @PostMapping("/templates/bulk")
    public List<TemplateView> importTemplates(@RequestBody List<TemplateView> body) {
        return app.importTemplates(body);
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
        return app.send(body);
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
