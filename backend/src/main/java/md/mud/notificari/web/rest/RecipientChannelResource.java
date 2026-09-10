package md.mud.notificari.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import md.mud.notificari.repository.RecipientChannelRepository;
import md.mud.notificari.service.RecipientChannelService;
import md.mud.notificari.service.dto.RecipientChannelDTO;
import md.mud.notificari.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link md.mud.notificari.domain.RecipientChannel}.
 */
@RestController
@RequestMapping("/api/recipient-channels")
public class RecipientChannelResource {

    private static final Logger LOG = LoggerFactory.getLogger(RecipientChannelResource.class);

    private static final String ENTITY_NAME = "recipientChannel";

    @Value("${jhipster.clientApp.name:notificariMud}")
    private String applicationName;

    private final RecipientChannelService recipientChannelService;

    private final RecipientChannelRepository recipientChannelRepository;

    public RecipientChannelResource(
        RecipientChannelService recipientChannelService,
        RecipientChannelRepository recipientChannelRepository
    ) {
        this.recipientChannelService = recipientChannelService;
        this.recipientChannelRepository = recipientChannelRepository;
    }

    /**
     * {@code POST  /recipient-channels} : Create a new recipientChannel.
     *
     * @param recipientChannelDTO the recipientChannelDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new recipientChannelDTO, or with status {@code 400 (Bad Request)} if the recipientChannel has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RecipientChannelDTO> createRecipientChannel(@Valid @RequestBody RecipientChannelDTO recipientChannelDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save RecipientChannel : {}", recipientChannelDTO);
        if (recipientChannelDTO.getId() != null) {
            throw new BadRequestAlertException("A new recipientChannel cannot already have an ID", ENTITY_NAME, "idexists");
        }
        recipientChannelDTO = recipientChannelService.save(recipientChannelDTO);
        return ResponseEntity.created(new URI("/api/recipient-channels/" + recipientChannelDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, recipientChannelDTO.getId().toString()))
            .body(recipientChannelDTO);
    }

    /**
     * {@code PUT  /recipient-channels/:id} : Updates an existing recipientChannel.
     *
     * @param id the id of the recipientChannelDTO to save.
     * @param recipientChannelDTO the recipientChannelDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated recipientChannelDTO,
     * or with status {@code 400 (Bad Request)} if the recipientChannelDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the recipientChannelDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecipientChannelDTO> updateRecipientChannel(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody RecipientChannelDTO recipientChannelDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update RecipientChannel : {}, {}", id, recipientChannelDTO);
        if (recipientChannelDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, recipientChannelDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!recipientChannelRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        recipientChannelDTO = recipientChannelService.update(recipientChannelDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, recipientChannelDTO.getId().toString()))
            .body(recipientChannelDTO);
    }

    /**
     * {@code PATCH  /recipient-channels/:id} : Partial updates given fields of an existing recipientChannel, field will ignore if it is null
     *
     * @param id the id of the recipientChannelDTO to save.
     * @param recipientChannelDTO the recipientChannelDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated recipientChannelDTO,
     * or with status {@code 400 (Bad Request)} if the recipientChannelDTO is not valid,
     * or with status {@code 404 (Not Found)} if the recipientChannelDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the recipientChannelDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RecipientChannelDTO> partialUpdateRecipientChannel(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody RecipientChannelDTO recipientChannelDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update RecipientChannel partially : {}, {}", id, recipientChannelDTO);
        if (recipientChannelDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, recipientChannelDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!recipientChannelRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RecipientChannelDTO> result = recipientChannelService.partialUpdate(recipientChannelDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, recipientChannelDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /recipient-channels} : get all the Recipient Channels.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Recipient Channels in body.
     */
    @GetMapping("")
    public List<RecipientChannelDTO> getAllRecipientChannels(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all RecipientChannels");
        return recipientChannelService.findAll();
    }

    /**
     * {@code GET  /recipient-channels/:id} : get the "id" recipientChannel.
     *
     * @param id the id of the recipientChannelDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the recipientChannelDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecipientChannelDTO> getRecipientChannel(@PathVariable("id") Long id) {
        LOG.debug("REST request to get RecipientChannel : {}", id);
        Optional<RecipientChannelDTO> recipientChannelDTO = recipientChannelService.findOne(id);
        return ResponseUtil.wrapOrNotFound(recipientChannelDTO);
    }

    /**
     * {@code DELETE  /recipient-channels/:id} : delete the "id" recipientChannel.
     *
     * @param id the id of the recipientChannelDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipientChannel(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete RecipientChannel : {}", id);
        recipientChannelService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
