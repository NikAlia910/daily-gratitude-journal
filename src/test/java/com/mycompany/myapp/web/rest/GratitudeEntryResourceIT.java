package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.GratitudeEntryAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.GratitudeEntry;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.Mood;
import com.mycompany.myapp.repository.GratitudeEntryRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.service.GratitudeEntryService;
import com.mycompany.myapp.service.dto.GratitudeEntryDTO;
import com.mycompany.myapp.service.mapper.GratitudeEntryMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link GratitudeEntryResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser("testuser")
class GratitudeEntryResourceIT {

    private static final LocalDate DEFAULT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final String DEFAULT_ENTRY = "AAAAAAAAAA";
    private static final String UPDATED_ENTRY = "BBBBBBBBBB";

    private static final Mood DEFAULT_MOOD = Mood.HAPPY;
    private static final Mood UPDATED_MOOD = Mood.GRATEFUL;

    private static final Instant DEFAULT_TIMESTAMP = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_TIMESTAMP = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/gratitude-entries";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GratitudeEntryRepository gratitudeEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private GratitudeEntryRepository gratitudeEntryRepositoryMock;

    @Autowired
    private GratitudeEntryMapper gratitudeEntryMapper;

    @Mock
    private GratitudeEntryService gratitudeEntryServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restGratitudeEntryMockMvc;

    private GratitudeEntry gratitudeEntry;
    private User testUser;

    private GratitudeEntry insertedGratitudeEntry;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GratitudeEntry createEntity() {
        return new GratitudeEntry().date(DEFAULT_DATE).entry(DEFAULT_ENTRY).mood(DEFAULT_MOOD).timestamp(DEFAULT_TIMESTAMP);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GratitudeEntry createUpdatedEntity() {
        return new GratitudeEntry().date(UPDATED_DATE).entry(UPDATED_ENTRY).mood(UPDATED_MOOD).timestamp(UPDATED_TIMESTAMP);
    }

    @BeforeEach
    void initTest() {
        // Create and save test user
        testUser = new User();
        testUser.setLogin("testuser");
        testUser.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setActivated(true);
        testUser.setLangKey("en");
        testUser = userRepository.save(testUser);

        gratitudeEntry = createEntity();
        gratitudeEntry.setUser(testUser);
    }

    @AfterEach
    void cleanup() {
        if (insertedGratitudeEntry != null) {
            gratitudeEntryRepository.delete(insertedGratitudeEntry);
            insertedGratitudeEntry = null;
        }
        if (testUser != null) {
            userRepository.delete(testUser);
        }
    }

    @Test
    @Transactional
    void createGratitudeEntry() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the GratitudeEntry
        GratitudeEntryDTO gratitudeEntryDTO = gratitudeEntryMapper.toDto(gratitudeEntry);
        var returnedGratitudeEntryDTO = om.readValue(
            restGratitudeEntryMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gratitudeEntryDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            GratitudeEntryDTO.class
        );

        // Validate the GratitudeEntry in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedGratitudeEntry = gratitudeEntryMapper.toEntity(returnedGratitudeEntryDTO);
        assertGratitudeEntryUpdatableFieldsEquals(returnedGratitudeEntry, getPersistedGratitudeEntry(returnedGratitudeEntry));

        insertedGratitudeEntry = returnedGratitudeEntry;
    }

    @Test
    @Transactional
    void createGratitudeEntryWithExistingId() throws Exception {
        // Create the GratitudeEntry with an existing ID
        gratitudeEntry.setId(1L);
        GratitudeEntryDTO gratitudeEntryDTO = gratitudeEntryMapper.toDto(gratitudeEntry);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gratitudeEntryDTO)))
            .andExpect(status().isBadRequest());

        // Validate the GratitudeEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        gratitudeEntry.setDate(null);

        // Create the GratitudeEntry, which fails.
        GratitudeEntryDTO gratitudeEntryDTO = gratitudeEntryMapper.toDto(gratitudeEntry);

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gratitudeEntryDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTimestampIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        gratitudeEntry.setTimestamp(null);

        // Create the GratitudeEntry, which fails.
        GratitudeEntryDTO gratitudeEntryDTO = gratitudeEntryMapper.toDto(gratitudeEntry);

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gratitudeEntryDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllGratitudeEntries() throws Exception {
        // Initialize the database
        insertedGratitudeEntry = gratitudeEntryRepository.saveAndFlush(gratitudeEntry);

        // Get all the gratitudeEntryList
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(gratitudeEntry.getId().intValue())))
            .andExpect(jsonPath("$.[*].date").value(hasItem(DEFAULT_DATE.toString())))
            .andExpect(jsonPath("$.[*].entry").value(hasItem(DEFAULT_ENTRY)))
            .andExpect(jsonPath("$.[*].mood").value(hasItem(DEFAULT_MOOD.toString())))
            .andExpect(jsonPath("$.[*].timestamp").value(hasItem(DEFAULT_TIMESTAMP.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllGratitudeEntriesWithEagerRelationshipsIsEnabled() throws Exception {
        when(gratitudeEntryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(gratitudeEntryServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllGratitudeEntriesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(gratitudeEntryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(gratitudeEntryRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getGratitudeEntry() throws Exception {
        // Initialize the database
        insertedGratitudeEntry = gratitudeEntryRepository.saveAndFlush(gratitudeEntry);

        // Get the gratitudeEntry
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL_ID, gratitudeEntry.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(gratitudeEntry.getId().intValue()))
            .andExpect(jsonPath("$.date").value(DEFAULT_DATE.toString()))
            .andExpect(jsonPath("$.entry").value(DEFAULT_ENTRY))
            .andExpect(jsonPath("$.mood").value(DEFAULT_MOOD.toString()))
            .andExpect(jsonPath("$.timestamp").value(DEFAULT_TIMESTAMP.toString()));
    }

    @Test
    @Transactional
    void getNonExistingGratitudeEntry() throws Exception {
        // Get the gratitudeEntry
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingGratitudeEntry() throws Exception {
        // Initialize the database
        insertedGratitudeEntry = gratitudeEntryRepository.saveAndFlush(gratitudeEntry);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the gratitudeEntry
        GratitudeEntry updatedGratitudeEntry = gratitudeEntryRepository.findById(gratitudeEntry.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedGratitudeEntry are not directly saved in db
        em.detach(updatedGratitudeEntry);
        // Note: timestamp should be preserved (not updated) according to business logic
        updatedGratitudeEntry.date(UPDATED_DATE).entry(UPDATED_ENTRY).mood(UPDATED_MOOD);
        GratitudeEntryDTO gratitudeEntryDTO = gratitudeEntryMapper.toDto(updatedGratitudeEntry);

        restGratitudeEntryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, gratitudeEntryDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(gratitudeEntryDTO))
            )
            .andExpect(status().isOk());

        // Validate the GratitudeEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        // Create expected entity with preserved timestamp for verification
        GratitudeEntry expectedGratitudeEntry = new GratitudeEntry()
            .id(updatedGratitudeEntry.getId())
            .date(UPDATED_DATE)
            .entry(UPDATED_ENTRY)
            .mood(UPDATED_MOOD)
            .timestamp(gratitudeEntry.getTimestamp()) // Original timestamp should be preserved
            .user(testUser);
        assertPersistedGratitudeEntryToMatchAllProperties(expectedGratitudeEntry);
    }

    @Test
    @Transactional
    void putNonExistingGratitudeEntry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gratitudeEntry.setId(longCount.incrementAndGet());

        // Create the GratitudeEntry
        GratitudeEntryDTO gratitudeEntryDTO = gratitudeEntryMapper.toDto(gratitudeEntry);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGratitudeEntryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, gratitudeEntryDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(gratitudeEntryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GratitudeEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchGratitudeEntry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gratitudeEntry.setId(longCount.incrementAndGet());

        // Create the GratitudeEntry
        GratitudeEntryDTO gratitudeEntryDTO = gratitudeEntryMapper.toDto(gratitudeEntry);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGratitudeEntryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(gratitudeEntryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GratitudeEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamGratitudeEntry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gratitudeEntry.setId(longCount.incrementAndGet());

        // Create the GratitudeEntry
        GratitudeEntryDTO gratitudeEntryDTO = gratitudeEntryMapper.toDto(gratitudeEntry);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGratitudeEntryMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gratitudeEntryDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the GratitudeEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateGratitudeEntryWithPatch() throws Exception {
        // Initialize the database
        insertedGratitudeEntry = gratitudeEntryRepository.saveAndFlush(gratitudeEntry);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the gratitudeEntry using partial update
        GratitudeEntry partialUpdatedGratitudeEntry = new GratitudeEntry();
        partialUpdatedGratitudeEntry.setId(gratitudeEntry.getId());

        partialUpdatedGratitudeEntry.entry(UPDATED_ENTRY).mood(UPDATED_MOOD);

        restGratitudeEntryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGratitudeEntry.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGratitudeEntry))
            )
            .andExpect(status().isOk());

        // Validate the GratitudeEntry in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGratitudeEntryUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedGratitudeEntry, gratitudeEntry),
            getPersistedGratitudeEntry(gratitudeEntry)
        );
    }

    @Test
    @Transactional
    void fullUpdateGratitudeEntryWithPatch() throws Exception {
        // Initialize the database
        insertedGratitudeEntry = gratitudeEntryRepository.saveAndFlush(gratitudeEntry);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the gratitudeEntry using partial update
        GratitudeEntry partialUpdatedGratitudeEntry = new GratitudeEntry();
        partialUpdatedGratitudeEntry.setId(gratitudeEntry.getId());

        partialUpdatedGratitudeEntry.date(UPDATED_DATE).entry(UPDATED_ENTRY).mood(UPDATED_MOOD);

        restGratitudeEntryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGratitudeEntry.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGratitudeEntry))
            )
            .andExpect(status().isOk());

        // Validate the GratitudeEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);

        // Create expected entity with preserved timestamp for verification
        GratitudeEntry expectedGratitudeEntry = new GratitudeEntry()
            .id(partialUpdatedGratitudeEntry.getId())
            .date(UPDATED_DATE)
            .entry(UPDATED_ENTRY)
            .mood(UPDATED_MOOD)
            .timestamp(gratitudeEntry.getTimestamp()) // Original timestamp should be preserved
            .user(testUser);
        assertGratitudeEntryUpdatableFieldsEquals(expectedGratitudeEntry, getPersistedGratitudeEntry(partialUpdatedGratitudeEntry));
    }

    @Test
    @Transactional
    void patchNonExistingGratitudeEntry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gratitudeEntry.setId(longCount.incrementAndGet());

        // Create the GratitudeEntry
        GratitudeEntryDTO gratitudeEntryDTO = gratitudeEntryMapper.toDto(gratitudeEntry);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGratitudeEntryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, gratitudeEntryDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(gratitudeEntryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GratitudeEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchGratitudeEntry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gratitudeEntry.setId(longCount.incrementAndGet());

        // Create the GratitudeEntry
        GratitudeEntryDTO gratitudeEntryDTO = gratitudeEntryMapper.toDto(gratitudeEntry);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGratitudeEntryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(gratitudeEntryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GratitudeEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamGratitudeEntry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gratitudeEntry.setId(longCount.incrementAndGet());

        // Create the GratitudeEntry
        GratitudeEntryDTO gratitudeEntryDTO = gratitudeEntryMapper.toDto(gratitudeEntry);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGratitudeEntryMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(gratitudeEntryDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the GratitudeEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteGratitudeEntry() throws Exception {
        // Initialize the database
        insertedGratitudeEntry = gratitudeEntryRepository.saveAndFlush(gratitudeEntry);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the gratitudeEntry
        restGratitudeEntryMockMvc
            .perform(delete(ENTITY_API_URL_ID, gratitudeEntry.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return gratitudeEntryRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected GratitudeEntry getPersistedGratitudeEntry(GratitudeEntry gratitudeEntry) {
        return gratitudeEntryRepository.findById(gratitudeEntry.getId()).orElseThrow();
    }

    protected void assertPersistedGratitudeEntryToMatchAllProperties(GratitudeEntry expectedGratitudeEntry) {
        assertGratitudeEntryAllPropertiesEquals(expectedGratitudeEntry, getPersistedGratitudeEntry(expectedGratitudeEntry));
    }

    protected void assertPersistedGratitudeEntryToMatchUpdatableProperties(GratitudeEntry expectedGratitudeEntry) {
        assertGratitudeEntryAllUpdatablePropertiesEquals(expectedGratitudeEntry, getPersistedGratitudeEntry(expectedGratitudeEntry));
    }
}
