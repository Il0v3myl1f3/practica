package md.mud.notificari.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import md.mud.notificari.repository.RecipientRepository;
import md.mud.notificari.service.RecipientQueryService;
import md.mud.notificari.service.RecipientService;
import md.mud.notificari.service.criteria.RecipientCriteria;
import md.mud.notificari.service.dto.RecipientDTO;
import md.mud.notificari.errors.BadRequestAlertException;
import md.mud.notificari.errors.RecipientException;
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
 * REST controller for managing {@link md.mud.notificari.domain.Recipient}.
 */
@RestController
@RequestMapping("/api/recipients")
public class RecipientResource {

    private static final Logger LOG = LoggerFactory.getLogger(RecipientResource.class);

    private static final String ENTITY_NAME = "recipient";

    @Value("${jhipster.clientApp.name:notificariMud}")
    private String applicationName;

    private final RecipientService recipientService;

    private final RecipientRepository recipientRepository;

    private final RecipientQueryService recipientQueryService;

    public RecipientResource(
        RecipientService recipientService,
        RecipientRepository recipientRepository,
        RecipientQueryService recipientQueryService
    ) {
        this.recipientService = recipientService;
        this.recipientRepository = recipientRepository;
        this.recipientQueryService = recipientQueryService;
    }

    /**
     * {@code POST  /recipients} : Create a new recipient.
     *
     * @param recipientDTO the recipientDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new recipientDTO, or with status {@code 400 (Bad Request)} if the recipient has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RecipientDTO> createRecipient(@Valid @RequestBody RecipientDTO recipientDTO) throws URISyntaxException {
        LOG.debug("REST request to save Recipient : {}", recipientDTO);
        if (recipientDTO.getId() != null) {
            throw new BadRequestAlertException("A new recipient cannot already have an ID", ENTITY_NAME, "idexists");
        }
        recipientDTO = recipientService.save(recipientDTO);
        return ResponseEntity.created(new URI("/api/recipients/" + recipientDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, recipientDTO.getId().toString()))
            .body(recipientDTO);
    }

    /**
     * {@code PUT  /recipients/:id} : Updates an existing recipient.
     *
     * @param id the id of the recipientDTO to save.
     * @param recipientDTO the recipientDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated recipientDTO,
     * or with status {@code 400 (Bad Request)} if the recipientDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the recipientDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecipientDTO> updateRecipient(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody RecipientDTO recipientDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Recipient : {}, {}", id, recipientDTO);
        if (recipientDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, recipientDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!recipientRepository.existsById(id)) {
            throw RecipientException.notFound(id);
        }

        recipientDTO = recipientService.update(recipientDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, recipientDTO.getId().toString()))
            .body(recipientDTO);
    }

    /**
     * {@code PATCH  /recipients/:id} : Partial updates given fields of an existing recipient, field will ignore if it is null
     *
     * @param id the id of the recipientDTO to save.
     * @param recipientDTO the recipientDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated recipientDTO,
     * or with status {@code 400 (Bad Request)} if the recipientDTO is not valid,
     * or with status {@code 404 (Not Found)} if the recipientDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the recipientDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RecipientDTO> partialUpdateRecipient(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody RecipientDTO recipientDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Recipient partially : {}, {}", id, recipientDTO);
        if (recipientDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, recipientDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!recipientRepository.existsById(id)) {
            throw RecipientException.notFound(id);
        }

        Optional<RecipientDTO> result = recipientService.partialUpdate(recipientDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, recipientDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /recipients} : get all the Recipients.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Recipients in body.
     */
    @GetMapping("")
    public ResponseEntity<List<RecipientDTO>> getAllRecipients(
        RecipientCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Recipients by criteria: {}", criteria);

        Page<RecipientDTO> page = recipientQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /recipients/count} : count all the recipients.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countRecipients(RecipientCriteria criteria) {
        LOG.debug("REST request to count Recipients by criteria: {}", criteria);
        return ResponseEntity.ok().body(recipientQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /recipients/:id} : get the "id" recipient.
     *
     * @param id the id of the recipientDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the recipientDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecipientDTO> getRecipient(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Recipient : {}", id);
        Optional<RecipientDTO> recipientDTO = recipientService.findOne(id);
        return ResponseUtil.wrapOrNotFound(recipientDTO);
    }

    /**
     * {@code DELETE  /recipients/:id} : delete the "id" recipient.
     *
     * @param id the id of the recipientDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipient(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Recipient : {}", id);
        recipientService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
