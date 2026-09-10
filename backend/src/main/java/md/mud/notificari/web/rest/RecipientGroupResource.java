package md.mud.notificari.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import md.mud.notificari.repository.RecipientGroupRepository;
import md.mud.notificari.service.RecipientGroupService;
import md.mud.notificari.service.dto.RecipientGroupDTO;
import md.mud.notificari.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link md.mud.notificari.domain.RecipientGroup}.
 */
@RestController
@RequestMapping("/api/recipient-groups")
public class RecipientGroupResource {

    private static final Logger LOG = LoggerFactory.getLogger(RecipientGroupResource.class);

    private static final String ENTITY_NAME = "recipientGroup";

    @Value("${jhipster.clientApp.name:notificariMud}")
    private String applicationName;

    private final RecipientGroupService recipientGroupService;

    private final RecipientGroupRepository recipientGroupRepository;

    public RecipientGroupResource(RecipientGroupService recipientGroupService, RecipientGroupRepository recipientGroupRepository) {
        this.recipientGroupService = recipientGroupService;
        this.recipientGroupRepository = recipientGroupRepository;
    }

    /**
     * {@code POST  /recipient-groups} : Create a new recipientGroup.
     *
     * @param recipientGroupDTO the recipientGroupDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new recipientGroupDTO, or with status {@code 400 (Bad Request)} if the recipientGroup has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RecipientGroupDTO> createRecipientGroup(@Valid @RequestBody RecipientGroupDTO recipientGroupDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save RecipientGroup : {}", recipientGroupDTO);
        if (recipientGroupDTO.getId() != null) {
            throw new BadRequestAlertException("A new recipientGroup cannot already have an ID", ENTITY_NAME, "idexists");
        }
        recipientGroupDTO = recipientGroupService.save(recipientGroupDTO);
        return ResponseEntity.created(new URI("/api/recipient-groups/" + recipientGroupDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, recipientGroupDTO.getId().toString()))
            .body(recipientGroupDTO);
    }

    /**
     * {@code PUT  /recipient-groups/:id} : Updates an existing recipientGroup.
     *
     * @param id the id of the recipientGroupDTO to save.
     * @param recipientGroupDTO the recipientGroupDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated recipientGroupDTO,
     * or with status {@code 400 (Bad Request)} if the recipientGroupDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the recipientGroupDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecipientGroupDTO> updateRecipientGroup(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody RecipientGroupDTO recipientGroupDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update RecipientGroup : {}, {}", id, recipientGroupDTO);
        if (recipientGroupDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, recipientGroupDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!recipientGroupRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        recipientGroupDTO = recipientGroupService.update(recipientGroupDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, recipientGroupDTO.getId().toString()))
            .body(recipientGroupDTO);
    }

    /**
     * {@code PATCH  /recipient-groups/:id} : Partial updates given fields of an existing recipientGroup, field will ignore if it is null
     *
     * @param id the id of the recipientGroupDTO to save.
     * @param recipientGroupDTO the recipientGroupDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated recipientGroupDTO,
     * or with status {@code 400 (Bad Request)} if the recipientGroupDTO is not valid,
     * or with status {@code 404 (Not Found)} if the recipientGroupDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the recipientGroupDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RecipientGroupDTO> partialUpdateRecipientGroup(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody RecipientGroupDTO recipientGroupDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update RecipientGroup partially : {}, {}", id, recipientGroupDTO);
        if (recipientGroupDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, recipientGroupDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!recipientGroupRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RecipientGroupDTO> result = recipientGroupService.partialUpdate(recipientGroupDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, recipientGroupDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /recipient-groups} : get all the Recipient Groups.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Recipient Groups in body.
     */
    @GetMapping("")
    public List<RecipientGroupDTO> getAllRecipientGroups(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all RecipientGroups");
        return recipientGroupService.findAll();
    }

    /**
     * {@code GET  /recipient-groups/:id} : get the "id" recipientGroup.
     *
     * @param id the id of the recipientGroupDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the recipientGroupDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecipientGroupDTO> getRecipientGroup(@PathVariable("id") Long id) {
        LOG.debug("REST request to get RecipientGroup : {}", id);
        Optional<RecipientGroupDTO> recipientGroupDTO = recipientGroupService.findOne(id);
        return ResponseUtil.wrapOrNotFound(recipientGroupDTO);
    }

    /**
     * {@code DELETE  /recipient-groups/:id} : delete the "id" recipientGroup.
     *
     * @param id the id of the recipientGroupDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipientGroup(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete RecipientGroup : {}", id);
        recipientGroupService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
