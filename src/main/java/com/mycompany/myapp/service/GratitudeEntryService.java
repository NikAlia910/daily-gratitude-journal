package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.GratitudeEntry;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.GratitudeEntryRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.dto.GratitudeEntryDTO;
import com.mycompany.myapp.service.mapper.GratitudeEntryMapper;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.GratitudeEntry}.
 */
@Service
@Transactional
public class GratitudeEntryService {

    private static final Logger LOG = LoggerFactory.getLogger(GratitudeEntryService.class);

    private final GratitudeEntryRepository gratitudeEntryRepository;

    private final GratitudeEntryMapper gratitudeEntryMapper;

    private final UserRepository userRepository;

    public GratitudeEntryService(
        GratitudeEntryRepository gratitudeEntryRepository,
        GratitudeEntryMapper gratitudeEntryMapper,
        UserRepository userRepository
    ) {
        this.gratitudeEntryRepository = gratitudeEntryRepository;
        this.gratitudeEntryMapper = gratitudeEntryMapper;
        this.userRepository = userRepository;
    }

    /**
     * Save a gratitudeEntry.
     *
     * @param gratitudeEntryDTO the entity to save.
     * @return the persisted entity.
     */
    public GratitudeEntryDTO save(GratitudeEntryDTO gratitudeEntryDTO) {
        LOG.debug("Request to save GratitudeEntry : {}", gratitudeEntryDTO);

        // Validate entry text is not empty
        if (!StringUtils.hasText(gratitudeEntryDTO.getEntry())) {
            throw new BadRequestAlertException("Entry text cannot be empty", "gratitudeEntry", "entryempty");
        }

        // Check if entry already exists for this date and current user
        if (gratitudeEntryDTO.getId() == null && gratitudeEntryRepository.existsByUserIsCurrentUserAndDate(gratitudeEntryDTO.getDate())) {
            throw new BadRequestAlertException("An entry already exists for this date", "gratitudeEntry", "dateexists");
        }

        // Validate that ID is not provided for new entries
        if (gratitudeEntryDTO.getId() != null) {
            throw new BadRequestAlertException("ID should not be provided for new entries", "gratitudeEntry", "idinvalid");
        }

        GratitudeEntry gratitudeEntry = gratitudeEntryMapper.toEntity(gratitudeEntryDTO);

        // Set current user
        SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin).ifPresent(gratitudeEntry::setUser);

        // Set timestamp if creating new entry
        if (gratitudeEntry.getId() == null) {
            gratitudeEntry.setTimestamp(Instant.now());
        }

        // Set date to today if not specified
        if (gratitudeEntry.getDate() == null) {
            gratitudeEntry.setDate(LocalDate.now());
        }

        gratitudeEntry = gratitudeEntryRepository.save(gratitudeEntry);
        return gratitudeEntryMapper.toDto(gratitudeEntry);
    }

    /**
     * Update a gratitudeEntry.
     *
     * @param gratitudeEntryDTO the entity to save.
     * @return the persisted entity.
     */
    public GratitudeEntryDTO update(GratitudeEntryDTO gratitudeEntryDTO) {
        LOG.debug("Request to update GratitudeEntry : {}", gratitudeEntryDTO);

        // Validate entry text is not empty
        if (!StringUtils.hasText(gratitudeEntryDTO.getEntry())) {
            throw new BadRequestAlertException("Entry text cannot be empty", "gratitudeEntry", "entryempty");
        }

        // Find existing entry to preserve timestamp and check ownership
        Optional<GratitudeEntry> existingEntryOpt = gratitudeEntryRepository.findById(gratitudeEntryDTO.getId());
        if (existingEntryOpt.isEmpty()) {
            throw new BadRequestAlertException("Entity not found", "gratitudeEntry", "idnotfound");
        }

        GratitudeEntry existingEntry = existingEntryOpt.orElseThrow();

        // Check if entry belongs to current user
        if (!belongsToCurrentUser(existingEntry)) {
            throw new BadRequestAlertException("Entry not found or does not belong to current user", "gratitudeEntry", "accessdenied");
        }

        GratitudeEntry gratitudeEntry = gratitudeEntryMapper.toEntity(gratitudeEntryDTO);

        // Preserve original timestamp and user
        gratitudeEntry.setTimestamp(existingEntry.getTimestamp());
        gratitudeEntry.setUser(existingEntry.getUser());

        gratitudeEntry = gratitudeEntryRepository.save(gratitudeEntry);
        return gratitudeEntryMapper.toDto(gratitudeEntry);
    }

    /**
     * Partially update a gratitudeEntry.
     *
     * @param gratitudeEntryDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<GratitudeEntryDTO> partialUpdate(GratitudeEntryDTO gratitudeEntryDTO) {
        LOG.debug("Request to partially update GratitudeEntry : {}", gratitudeEntryDTO);

        return gratitudeEntryRepository
            .findById(gratitudeEntryDTO.getId())
            .map(existingGratitudeEntry -> {
                gratitudeEntryMapper.partialUpdate(existingGratitudeEntry, gratitudeEntryDTO);
                return existingGratitudeEntry;
            })
            .map(gratitudeEntryRepository::save)
            .map(gratitudeEntryMapper::toDto);
    }

    /**
     * Get all the gratitudeEntries for the current user.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<GratitudeEntryDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all GratitudeEntries for current user");
        return gratitudeEntryRepository.findByUserIsCurrentUser(pageable).map(gratitudeEntryMapper::toDto);
    }

    /**
     * Get all the gratitudeEntries for the current user with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<GratitudeEntryDTO> findAllWithEagerRelationships(Pageable pageable) {
        return gratitudeEntryRepository.findByUserIsCurrentUser(pageable).map(gratitudeEntryMapper::toDto);
    }

    /**
     * Get all the gratitudeEntries for the current user.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<GratitudeEntryDTO> findAllForCurrentUser() {
        LOG.debug("Request to get all GratitudeEntries for current user");
        return gratitudeEntryRepository
            .findByUserIsCurrentUser(PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "date", "timestamp")))
            .getContent()
            .stream()
            .map(gratitudeEntryMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Get gratitudeEntries for the current user within a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<GratitudeEntryDTO> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LOG.debug("Request to get GratitudeEntries for current user between {} and {}", startDate, endDate);
        return gratitudeEntryRepository
            .findByUserIsCurrentUserAndDateBetween(startDate, endDate, pageable)
            .map(gratitudeEntryMapper::toDto);
    }

    /**
     * Get the gratitude entry for the current user on a specific date.
     *
     * @param date the date
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<GratitudeEntryDTO> findByDate(LocalDate date) {
        LOG.debug("Request to get GratitudeEntry for current user on date {}", date);
        return gratitudeEntryRepository.findByUserIsCurrentUserAndDate(date).map(gratitudeEntryMapper::toDto);
    }

    /**
     * Get one gratitudeEntry by id (only if it belongs to current user).
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<GratitudeEntryDTO> findOne(Long id) {
        LOG.debug("Request to get GratitudeEntry : {}", id);
        return gratitudeEntryRepository
            .findOneWithEagerRelationships(id)
            .filter(this::belongsToCurrentUser)
            .map(gratitudeEntryMapper::toDto);
    }

    /**
     * Delete the gratitudeEntry by id (only if it belongs to current user).
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete GratitudeEntry : {}", id);
        Optional<GratitudeEntry> entryOpt = gratitudeEntryRepository.findById(id);
        if (entryOpt.isPresent() && belongsToCurrentUser(entryOpt.orElseThrow())) {
            gratitudeEntryRepository.deleteById(id);
        } else {
            throw new BadRequestAlertException("Entry not found or does not belong to current user", "gratitudeEntry", "accessdenied");
        }
    }

    /**
     * Check if the gratitude entry belongs to the current user.
     */
    private boolean belongsToCurrentUser(GratitudeEntry entry) {
        return SecurityUtils.getCurrentUserLogin().map(login -> login.equals(entry.getUser().getLogin())).orElse(false);
    }
}
