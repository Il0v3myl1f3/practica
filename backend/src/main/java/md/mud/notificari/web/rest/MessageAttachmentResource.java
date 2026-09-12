package md.mud.notificari.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import md.mud.notificari.repository.MessageAttachmentRepository;
import md.mud.notificari.service.MessageAttachmentService;
import md.mud.notificari.service.dto.MessageAttachmentDTO;
import md.mud.notificari.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link md.mud.notificari.domain.MessageAttachment}.
 */
@RestController
@RequestMapping("/api/message-attachments")
public class MessageAttachmentResource {

    private static final Logger LOG = LoggerFactory.getLogger(MessageAttachmentResource.class);

    private static final String ENTITY_NAME = "messageAttachment";

    @Value("${jhipster.clientApp.name:notificariMud}")
    private String applicationName;

    private final MessageAttachmentService messageAttachmentService;

    private final MessageAttachmentRepository messageAttachmentRepository;

    public MessageAttachmentResource(
        MessageAttachmentService messageAttachmentService,
        MessageAttachmentRepository messageAttachmentRepository
    ) {
        this.messageAttachmentService = messageAttachmentService;
        this.messageAttachmentRepository = messageAttachmentRepository;
    }

    /**
     * {@code POST  /message-attachments} : Create a new messageAttachment.
     *
     * @param messageAttachmentDTO the messageAttachmentDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new messageAttachmentDTO, or with status {@code 400 (Bad Request)} if the messageAttachment has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<MessageAttachmentDTO> createMessageAttachment(@Valid @RequestBody MessageAttachmentDTO messageAttachmentDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save MessageAttachment : {}", messageAttachmentDTO);
        if (messageAttachmentDTO.getId() != null) {
            throw new BadRequestAlertException("A new messageAttachment cannot already have an ID", ENTITY_NAME, "idexists");
        }
        messageAttachmentDTO = messageAttachmentService.save(messageAttachmentDTO);
        return ResponseEntity.created(new URI("/api/message-attachments/" + messageAttachmentDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, messageAttachmentDTO.getId().toString()))
            .body(messageAttachmentDTO);
    }

    /**
     * {@code PUT  /message-attachments/:id} : Updates an existing messageAttachment.
     *
     * @param id the id of the messageAttachmentDTO to save.
     * @param messageAttachmentDTO the messageAttachmentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated messageAttachmentDTO,
     * or with status {@code 400 (Bad Request)} if the messageAttachmentDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the messageAttachmentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MessageAttachmentDTO> updateMessageAttachment(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody MessageAttachmentDTO messageAttachmentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update MessageAttachment : {}, {}", id, messageAttachmentDTO);
        if (messageAttachmentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, messageAttachmentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!messageAttachmentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        messageAttachmentDTO = messageAttachmentService.update(messageAttachmentDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, messageAttachmentDTO.getId().toString()))
            .body(messageAttachmentDTO);
    }

    /**
     * {@code PATCH  /message-attachments/:id} : Partial updates given fields of an existing messageAttachment, field will ignore if it is null
     *
     * @param id the id of the messageAttachmentDTO to save.
     * @param messageAttachmentDTO the messageAttachmentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated messageAttachmentDTO,
     * or with status {@code 400 (Bad Request)} if the messageAttachmentDTO is not valid,
     * or with status {@code 404 (Not Found)} if the messageAttachmentDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the messageAttachmentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<MessageAttachmentDTO> partialUpdateMessageAttachment(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody MessageAttachmentDTO messageAttachmentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update MessageAttachment partially : {}, {}", id, messageAttachmentDTO);
        if (messageAttachmentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, messageAttachmentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!messageAttachmentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<MessageAttachmentDTO> result = messageAttachmentService.partialUpdate(messageAttachmentDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, messageAttachmentDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /message-attachments} : get all the Message Attachments.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Message Attachments in body.
     */
    @GetMapping("")
    public List<MessageAttachmentDTO> getAllMessageAttachments(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all MessageAttachments");
        return messageAttachmentService.findAll();
    }

    /**
     * {@code GET  /message-attachments/:id} : get the "id" messageAttachment.
     *
     * @param id the id of the messageAttachmentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the messageAttachmentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MessageAttachmentDTO> getMessageAttachment(@PathVariable("id") Long id) {
        LOG.debug("REST request to get MessageAttachment : {}", id);
        Optional<MessageAttachmentDTO> messageAttachmentDTO = messageAttachmentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(messageAttachmentDTO);
    }

    /**
     * {@code DELETE  /message-attachments/:id} : delete the "id" messageAttachment.
     *
     * @param id the id of the messageAttachmentDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessageAttachment(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete MessageAttachment : {}", id);
        messageAttachmentService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
