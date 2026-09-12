package md.mud.notificari.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import md.mud.notificari.repository.MembershipRepository;
import md.mud.notificari.service.MembershipService;
import md.mud.notificari.service.dto.MembershipDTO;
import md.mud.notificari.errors.BadRequestAlertException;
import md.mud.notificari.errors.MembershipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link md.mud.notificari.domain.Membership}.
 */
@RestController
@RequestMapping("/api/memberships")
public class MembershipResource {

    private static final Logger LOG = LoggerFactory.getLogger(MembershipResource.class);

    private static final String ENTITY_NAME = "membership";

    @Value("${jhipster.clientApp.name:notificariMud}")
    private String applicationName;

    private final MembershipService membershipService;

    private final MembershipRepository membershipRepository;

    public MembershipResource(MembershipService membershipService, MembershipRepository membershipRepository) {
        this.membershipService = membershipService;
        this.membershipRepository = membershipRepository;
    }

    /**
     * {@code POST  /memberships} : Create a new membership.
     *
     * @param membershipDTO the membershipDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new membershipDTO, or with status {@code 400 (Bad Request)} if the membership has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<MembershipDTO> createMembership(@Valid @RequestBody MembershipDTO membershipDTO) throws URISyntaxException {
        LOG.debug("REST request to save Membership : {}", membershipDTO);
        if (membershipDTO.getId() != null) {
            throw new BadRequestAlertException("A new membership cannot already have an ID", ENTITY_NAME, "idexists");
        }
        membershipDTO = membershipService.save(membershipDTO);
        return ResponseEntity.created(new URI("/api/memberships/" + membershipDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, membershipDTO.getId().toString()))
            .body(membershipDTO);
    }

    /**
     * {@code PUT  /memberships/:id} : Updates an existing membership.
     *
     * @param id the id of the membershipDTO to save.
     * @param membershipDTO the membershipDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated membershipDTO,
     * or with status {@code 400 (Bad Request)} if the membershipDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the membershipDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MembershipDTO> updateMembership(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody MembershipDTO membershipDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Membership : {}, {}", id, membershipDTO);
        if (membershipDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, membershipDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!membershipRepository.existsById(id)) {
            throw MembershipException.notFound(id);
        }

        membershipDTO = membershipService.update(membershipDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, membershipDTO.getId().toString()))
            .body(membershipDTO);
    }

    /**
     * {@code PATCH  /memberships/:id} : Partial updates given fields of an existing membership, field will ignore if it is null
     *
     * @param id the id of the membershipDTO to save.
     * @param membershipDTO the membershipDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated membershipDTO,
     * or with status {@code 400 (Bad Request)} if the membershipDTO is not valid,
     * or with status {@code 404 (Not Found)} if the membershipDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the membershipDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<MembershipDTO> partialUpdateMembership(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody MembershipDTO membershipDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Membership partially : {}, {}", id, membershipDTO);
        if (membershipDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, membershipDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!membershipRepository.existsById(id)) {
            throw MembershipException.notFound(id);
        }

        Optional<MembershipDTO> result = membershipService.partialUpdate(membershipDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, membershipDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /memberships} : get all the Memberships.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Memberships in body.
     */
    @GetMapping("")
    public List<MembershipDTO> getAllMemberships(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all Memberships");
        return membershipService.findAll();
    }

    /**
     * {@code GET  /memberships/:id} : get the "id" membership.
     *
     * @param id the id of the membershipDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the membershipDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MembershipDTO> getMembership(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Membership : {}", id);
        Optional<MembershipDTO> membershipDTO = membershipService.findOne(id);
        return ResponseUtil.wrapOrNotFound(membershipDTO);
    }

    /**
     * {@code DELETE  /memberships/:id} : delete the "id" membership.
     *
     * @param id the id of the membershipDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMembership(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Membership : {}", id);
        membershipService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
