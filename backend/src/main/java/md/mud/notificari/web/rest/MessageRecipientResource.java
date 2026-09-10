package md.mud.notificari.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import md.mud.notificari.repository.MessageRecipientRepository;
import md.mud.notificari.service.MessageRecipientService;
import md.mud.notificari.service.dto.MessageRecipientDTO;
import md.mud.notificari.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link md.mud.notificari.domain.MessageRecipient}.
 */
@RestController
@RequestMapping("/api/message-recipients")
public class MessageRecipientResource {

    private static final Logger LOG = LoggerFactory.getLogger(MessageRecipientResource.class);

    private static final String ENTITY_NAME = "messageRecipient";

    @Value("${jhipster.clientApp.name:notificariMud}")
    private String applicationName;

    private final MessageRecipientService messageRecipientService;

    private final MessageRecipientRepository messageRecipientRepository;

    public MessageRecipientResource(
        MessageRecipientService messageRecipientService,
        MessageRecipientRepository messageRecipientRepository
    ) {
        this.messageRecipientService = messageRecipientService;
        this.messageRecipientRepository = messageRecipientRepository;
    }

    /**
     * {@code POST  /message-recipients} : Create a new messageRecipient.
     *
     * @param messageRecipientDTO the messageRecipientDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new messageRecipientDTO, or with status {@code 400 (Bad Request)} if the messageRecipient has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<MessageRecipientDTO> createMessageRecipient(@Valid @RequestBody MessageRecipientDTO messageRecipientDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save MessageRecipient : {}", messageRecipientDTO);
        if (messageRecipientDTO.getId() != null) {
            throw new BadRequestAlertException("A new messageRecipient cannot already have an ID", ENTITY_NAME, "idexists");
        }
        messageRecipientDTO = messageRecipientService.save(messageRecipientDTO);
        return ResponseEntity.created(new URI("/api/message-recipients/" + messageRecipientDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, messageRecipientDTO.getId().toString()))
            .body(messageRecipientDTO);
    }

    /**
     * {@code PUT  /message-recipients/:id} : Updates an existing messageRecipient.
     *
     * @param id the id of the messageRecipientDTO to save.
     * @param messageRecipientDTO the messageRecipientDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated messageRecipientDTO,
     * or with status {@code 400 (Bad Request)} if the messageRecipientDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the messageRecipientDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MessageRecipientDTO> updateMessageRecipient(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody MessageRecipientDTO messageRecipientDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update MessageRecipient : {}, {}", id, messageRecipientDTO);
        if (messageRecipientDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, messageRecipientDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!messageRecipientRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        messageRecipientDTO = messageRecipientService.update(messageRecipientDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, messageRecipientDTO.getId().toString()))
            .body(messageRecipientDTO);
    }

    /**
     * {@code PATCH  /message-recipients/:id} : Partial updates given fields of an existing messageRecipient, field will ignore if it is null
     *
     * @param id the id of the messageRecipientDTO to save.
     * @param messageRecipientDTO the messageRecipientDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated messageRecipientDTO,
     * or with status {@code 400 (Bad Request)} if the messageRecipientDTO is not valid,
     * or with status {@code 404 (Not Found)} if the messageRecipientDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the messageRecipientDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<MessageRecipientDTO> partialUpdateMessageRecipient(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody MessageRecipientDTO messageRecipientDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update MessageRecipient partially : {}, {}", id, messageRecipientDTO);
        if (messageRecipientDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, messageRecipientDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!messageRecipientRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<MessageRecipientDTO> result = messageRecipientService.partialUpdate(messageRecipientDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, messageRecipientDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /message-recipients} : get all the Message Recipients.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Message Recipients in body.
     */
    @GetMapping("")
    public ResponseEntity<List<MessageRecipientDTO>> getAllMessageRecipients(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of MessageRecipients");
        Page<MessageRecipientDTO> page;
        if (eagerload) {
            page = messageRecipientService.findAllWithEagerRelationships(pageable);
        } else {
            page = messageRecipientService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /message-recipients/:id} : get the "id" messageRecipient.
     *
     * @param id the id of the messageRecipientDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the messageRecipientDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MessageRecipientDTO> getMessageRecipient(@PathVariable("id") Long id) {
        LOG.debug("REST request to get MessageRecipient : {}", id);
        Optional<MessageRecipientDTO> messageRecipientDTO = messageRecipientService.findOne(id);
        return ResponseUtil.wrapOrNotFound(messageRecipientDTO);
    }

    /**
     * {@code DELETE  /message-recipients/:id} : delete the "id" messageRecipient.
     *
     * @param id the id of the messageRecipientDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessageRecipient(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete MessageRecipient : {}", id);
        messageRecipientService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
