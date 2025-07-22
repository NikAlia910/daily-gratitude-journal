package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.GratitudeEntryRepository;
import com.mycompany.myapp.service.GratitudeEntryService;
import com.mycompany.myapp.service.dto.GratitudeEntryDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.GratitudeEntry}.
 */
@RestController
@RequestMapping("/api/gratitude-entries")
public class GratitudeEntryResource {

    private static final Logger LOG = LoggerFactory.getLogger(GratitudeEntryResource.class);

    private static final String ENTITY_NAME = "gratitudeEntry";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final GratitudeEntryService gratitudeEntryService;

    private final GratitudeEntryRepository gratitudeEntryRepository;

    public GratitudeEntryResource(GratitudeEntryService gratitudeEntryService, GratitudeEntryRepository gratitudeEntryRepository) {
        this.gratitudeEntryService = gratitudeEntryService;
        this.gratitudeEntryRepository = gratitudeEntryRepository;
    }

    /**
     * {@code POST  /gratitude-entries} : Create a new gratitudeEntry.
     *
     * @param gratitudeEntryDTO the gratitudeEntryDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new gratitudeEntryDTO, or with status {@code 400 (Bad Request)} if the gratitudeEntry has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<GratitudeEntryDTO> createGratitudeEntry(@Valid @RequestBody GratitudeEntryDTO gratitudeEntryDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save GratitudeEntry : {}", gratitudeEntryDTO);
        if (gratitudeEntryDTO.getId() != null) {
            throw new BadRequestAlertException("A new gratitudeEntry cannot already have an ID", ENTITY_NAME, "idexists");
        }
        gratitudeEntryDTO = gratitudeEntryService.save(gratitudeEntryDTO);
        return ResponseEntity.created(new URI("/api/gratitude-entries/" + gratitudeEntryDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, gratitudeEntryDTO.getId().toString()))
            .body(gratitudeEntryDTO);
    }

    /**
     * {@code PUT  /gratitude-entries/:id} : Updates an existing gratitudeEntry.
     *
     * @param id the id of the gratitudeEntryDTO to save.
     * @param gratitudeEntryDTO the gratitudeEntryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated gratitudeEntryDTO,
     * or with status {@code 400 (Bad Request)} if the gratitudeEntryDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the gratitudeEntryDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<GratitudeEntryDTO> updateGratitudeEntry(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody GratitudeEntryDTO gratitudeEntryDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update GratitudeEntry : {}, {}", id, gratitudeEntryDTO);
        if (gratitudeEntryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, gratitudeEntryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!gratitudeEntryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        gratitudeEntryDTO = gratitudeEntryService.update(gratitudeEntryDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, gratitudeEntryDTO.getId().toString()))
            .body(gratitudeEntryDTO);
    }

    /**
     * {@code PATCH  /gratitude-entries/:id} : Partial updates given fields of an existing gratitudeEntry, field will ignore if it is null
     *
     * @param id the id of the gratitudeEntryDTO to save.
     * @param gratitudeEntryDTO the gratitudeEntryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated gratitudeEntryDTO,
     * or with status {@code 400 (Bad Request)} if the gratitudeEntryDTO is not valid,
     * or with status {@code 404 (Not Found)} if the gratitudeEntryDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the gratitudeEntryDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<GratitudeEntryDTO> partialUpdateGratitudeEntry(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody GratitudeEntryDTO gratitudeEntryDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update GratitudeEntry partially : {}, {}", id, gratitudeEntryDTO);
        if (gratitudeEntryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, gratitudeEntryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!gratitudeEntryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<GratitudeEntryDTO> result = gratitudeEntryService.partialUpdate(gratitudeEntryDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, gratitudeEntryDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /gratitude-entries} : get all the gratitudeEntries.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of gratitudeEntries in body.
     */
    @GetMapping("")
    public ResponseEntity<List<GratitudeEntryDTO>> getAllGratitudeEntries(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of GratitudeEntries");
        Page<GratitudeEntryDTO> page;
        if (eagerload) {
            page = gratitudeEntryService.findAllWithEagerRelationships(pageable);
        } else {
            page = gratitudeEntryService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /gratitude-entries/:id} : get the "id" gratitudeEntry.
     *
     * @param id the id of the gratitudeEntryDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the gratitudeEntryDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GratitudeEntryDTO> getGratitudeEntry(@PathVariable("id") Long id) {
        LOG.debug("REST request to get GratitudeEntry : {}", id);
        Optional<GratitudeEntryDTO> gratitudeEntryDTO = gratitudeEntryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(gratitudeEntryDTO);
    }

    /**
     * {@code DELETE  /gratitude-entries/:id} : delete the "id" gratitudeEntry.
     *
     * @param id the id of the gratitudeEntryDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGratitudeEntry(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete GratitudeEntry : {}", id);
        gratitudeEntryService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
