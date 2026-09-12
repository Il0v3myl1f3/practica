package md.mud.notificari.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import md.mud.notificari.repository.MessageChannelRepository;
import md.mud.notificari.service.MessageChannelService;
import md.mud.notificari.service.dto.MessageChannelDTO;
import md.mud.notificari.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link md.mud.notificari.domain.MessageChannel}.
 */
@RestController
@RequestMapping("/api/message-channels")
public class MessageChannelResource {

    private static final Logger LOG = LoggerFactory.getLogger(MessageChannelResource.class);

    private static final String ENTITY_NAME = "messageChannel";

    @Value("${jhipster.clientApp.name:notificariMud}")
    private String applicationName;

    private final MessageChannelService messageChannelService;

    private final MessageChannelRepository messageChannelRepository;

    public MessageChannelResource(MessageChannelService messageChannelService, MessageChannelRepository messageChannelRepository) {
        this.messageChannelService = messageChannelService;
        this.messageChannelRepository = messageChannelRepository;
    }

    /**
     * {@code POST  /message-channels} : Create a new messageChannel.
     *
     * @param messageChannelDTO the messageChannelDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new messageChannelDTO, or with status {@code 400 (Bad Request)} if the messageChannel has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<MessageChannelDTO> createMessageChannel(@Valid @RequestBody MessageChannelDTO messageChannelDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save MessageChannel : {}", messageChannelDTO);
        if (messageChannelDTO.getId() != null) {
            throw new BadRequestAlertException("A new messageChannel cannot already have an ID", ENTITY_NAME, "idexists");
        }
        messageChannelDTO = messageChannelService.save(messageChannelDTO);
        return ResponseEntity.created(new URI("/api/message-channels/" + messageChannelDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, messageChannelDTO.getId().toString()))
            .body(messageChannelDTO);
    }

    /**
     * {@code PUT  /message-channels/:id} : Updates an existing messageChannel.
     *
     * @param id the id of the messageChannelDTO to save.
     * @param messageChannelDTO the messageChannelDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated messageChannelDTO,
     * or with status {@code 400 (Bad Request)} if the messageChannelDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the messageChannelDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MessageChannelDTO> updateMessageChannel(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody MessageChannelDTO messageChannelDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update MessageChannel : {}, {}", id, messageChannelDTO);
        if (messageChannelDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, messageChannelDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!messageChannelRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        messageChannelDTO = messageChannelService.update(messageChannelDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, messageChannelDTO.getId().toString()))
            .body(messageChannelDTO);
    }

    /**
     * {@code PATCH  /message-channels/:id} : Partial updates given fields of an existing messageChannel, field will ignore if it is null
     *
     * @param id the id of the messageChannelDTO to save.
     * @param messageChannelDTO the messageChannelDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated messageChannelDTO,
     * or with status {@code 400 (Bad Request)} if the messageChannelDTO is not valid,
     * or with status {@code 404 (Not Found)} if the messageChannelDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the messageChannelDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<MessageChannelDTO> partialUpdateMessageChannel(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody MessageChannelDTO messageChannelDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update MessageChannel partially : {}, {}", id, messageChannelDTO);
        if (messageChannelDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, messageChannelDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!messageChannelRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<MessageChannelDTO> result = messageChannelService.partialUpdate(messageChannelDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, messageChannelDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /message-channels} : get all the Message Channels.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Message Channels in body.
     */
    @GetMapping("")
    public List<MessageChannelDTO> getAllMessageChannels(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all MessageChannels");
        return messageChannelService.findAll();
    }

    /**
     * {@code GET  /message-channels/:id} : get the "id" messageChannel.
     *
     * @param id the id of the messageChannelDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the messageChannelDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MessageChannelDTO> getMessageChannel(@PathVariable("id") Long id) {
        LOG.debug("REST request to get MessageChannel : {}", id);
        Optional<MessageChannelDTO> messageChannelDTO = messageChannelService.findOne(id);
        return ResponseUtil.wrapOrNotFound(messageChannelDTO);
    }

    /**
     * {@code DELETE  /message-channels/:id} : delete the "id" messageChannel.
     *
     * @param id the id of the messageChannelDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessageChannel(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete MessageChannel : {}", id);
        messageChannelService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
