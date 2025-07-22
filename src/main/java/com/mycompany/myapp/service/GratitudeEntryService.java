package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.GratitudeEntry;
import com.mycompany.myapp.repository.GratitudeEntryRepository;
import com.mycompany.myapp.service.dto.GratitudeEntryDTO;
import com.mycompany.myapp.service.mapper.GratitudeEntryMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.GratitudeEntry}.
 */
@Service
@Transactional
public class GratitudeEntryService {

    private static final Logger LOG = LoggerFactory.getLogger(GratitudeEntryService.class);

    private final GratitudeEntryRepository gratitudeEntryRepository;

    private final GratitudeEntryMapper gratitudeEntryMapper;

    public GratitudeEntryService(GratitudeEntryRepository gratitudeEntryRepository, GratitudeEntryMapper gratitudeEntryMapper) {
        this.gratitudeEntryRepository = gratitudeEntryRepository;
        this.gratitudeEntryMapper = gratitudeEntryMapper;
    }

    /**
     * Save a gratitudeEntry.
     *
     * @param gratitudeEntryDTO the entity to save.
     * @return the persisted entity.
     */
    public GratitudeEntryDTO save(GratitudeEntryDTO gratitudeEntryDTO) {
        LOG.debug("Request to save GratitudeEntry : {}", gratitudeEntryDTO);
        GratitudeEntry gratitudeEntry = gratitudeEntryMapper.toEntity(gratitudeEntryDTO);
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
        GratitudeEntry gratitudeEntry = gratitudeEntryMapper.toEntity(gratitudeEntryDTO);
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
     * Get all the gratitudeEntries.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<GratitudeEntryDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all GratitudeEntries");
        return gratitudeEntryRepository.findAll(pageable).map(gratitudeEntryMapper::toDto);
    }

    /**
     * Get all the gratitudeEntries with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<GratitudeEntryDTO> findAllWithEagerRelationships(Pageable pageable) {
        return gratitudeEntryRepository.findAllWithEagerRelationships(pageable).map(gratitudeEntryMapper::toDto);
    }

    /**
     * Get one gratitudeEntry by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<GratitudeEntryDTO> findOne(Long id) {
        LOG.debug("Request to get GratitudeEntry : {}", id);
        return gratitudeEntryRepository.findOneWithEagerRelationships(id).map(gratitudeEntryMapper::toDto);
    }

    /**
     * Delete the gratitudeEntry by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete GratitudeEntry : {}", id);
        gratitudeEntryRepository.deleteById(id);
    }
}
