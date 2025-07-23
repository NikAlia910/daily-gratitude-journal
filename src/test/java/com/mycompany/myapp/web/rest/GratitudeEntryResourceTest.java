package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.GratitudeEntry;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.Mood;
import com.mycompany.myapp.repository.GratitudeEntryRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.service.dto.GratitudeEntryDTO;
import com.mycompany.myapp.service.mapper.GratitudeEntryMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link GratitudeEntryResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(value = "testuser")
class GratitudeEntryResourceTest {

    private static final LocalDate DEFAULT_DATE = LocalDate.of(2024, 1, 1);
    private static final LocalDate UPDATED_DATE = LocalDate.of(2024, 1, 2);

    private static final String DEFAULT_ENTRY = "I am grateful for my health";
    private static final String UPDATED_ENTRY = "I am grateful for my family";

    private static final Mood DEFAULT_MOOD = Mood.GRATEFUL;
    private static final Mood UPDATED_MOOD = Mood.HAPPY;

    private static final Instant DEFAULT_TIMESTAMP = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_TIMESTAMP = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/gratitude-entries";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static AtomicLong longCount = new AtomicLong(new Random().nextLong());

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GratitudeEntryRepository gratitudeEntryRepository;

    @Autowired
    private GratitudeEntryMapper gratitudeEntryMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restGratitudeEntryMockMvc;

    @Autowired
    private UserRepository userRepository;

    private GratitudeEntry gratitudeEntry;
    private User testUser;

    /**
     * Create an entity for this test.
     */
    public static GratitudeEntry createEntity() {
        return new GratitudeEntry().date(DEFAULT_DATE).entry(DEFAULT_ENTRY).mood(DEFAULT_MOOD).timestamp(DEFAULT_TIMESTAMP);
    }

    /**
     * Create an updated entity for this test.
     */
    public static GratitudeEntry createUpdatedEntity() {
        return new GratitudeEntry().date(UPDATED_DATE).entry(UPDATED_ENTRY).mood(UPDATED_MOOD).timestamp(UPDATED_TIMESTAMP);
    }

    @BeforeEach
    public void initTest() {
        // Create test user
        testUser = new User();
        testUser.setLogin("testuser");
        testUser.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        testUser.setEmail("testuser@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setActivated(true);
        testUser.setLangKey("en");
        testUser = userRepository.save(testUser);

        gratitudeEntry = createEntity();
        gratitudeEntry.setUser(testUser);
    }

    @AfterEach
    public void cleanUp() {
        gratitudeEntryRepository.deleteAll();
        userRepository.delete(testUser);
    }

    @Test
    @Transactional
    void createGratitudeEntry() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();

        // Create the GratitudeEntry
        GratitudeEntryDTO gratitudeEntryDTO = gratitudeEntryMapper.toDto(gratitudeEntry);
        gratitudeEntryDTO.setId(null); // Remove ID for creation

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gratitudeEntryDTO)))
            .andExpect(status().isCreated());

        // Validate the GratitudeEntry in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);

        List<GratitudeEntry> gratitudeEntryList = gratitudeEntryRepository.findAll();
        GratitudeEntry testGratitudeEntry = gratitudeEntryList.get(gratitudeEntryList.size() - 1);
        assertThat(testGratitudeEntry.getDate()).isEqualTo(DEFAULT_DATE);
        assertThat(testGratitudeEntry.getEntry()).isEqualTo(DEFAULT_ENTRY);
        assertThat(testGratitudeEntry.getMood()).isEqualTo(DEFAULT_MOOD);
        assertThat(testGratitudeEntry.getTimestamp()).isNotNull();
        assertThat(testGratitudeEntry.getUser()).isNotNull();
        assertThat(testGratitudeEntry.getUser().getLogin()).isEqualTo("testuser");
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
    void createGratitudeEntryWithEmptyText() throws Exception {
        // Create the GratitudeEntry with empty entry text
        gratitudeEntry.setEntry("");
        GratitudeEntryDTO gratitudeEntryDTO = gratitudeEntryMapper.toDto(gratitudeEntry);
        gratitudeEntryDTO.setId(null);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // Creating entry with empty text should fail
        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gratitudeEntryDTO)))
            .andExpect(status().isBadRequest());

        // Validate the GratitudeEntry was not created
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void createDuplicateGratitudeEntryForSameDate() throws Exception {
        // Create first entry
        gratitudeEntryRepository.saveAndFlush(gratitudeEntry);

        // Try to create another entry for the same date
        GratitudeEntry duplicateEntry = createEntity();
        duplicateEntry.setUser(testUser);
        duplicateEntry.setEntry("Another entry for the same date");
        GratitudeEntryDTO duplicateEntryDTO = gratitudeEntryMapper.toDto(duplicateEntry);
        duplicateEntryDTO.setId(null);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // Creating duplicate entry for same date should fail
        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(duplicateEntryDTO)))
            .andExpect(status().isBadRequest());

        // Validate no new entry was created
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllGratitudeEntries() throws Exception {
        // Initialize the database
        gratitudeEntry = gratitudeEntryRepository.saveAndFlush(gratitudeEntry);

        // Get all the gratitudeEntryList
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(gratitudeEntry.getId().intValue())))
            .andExpect(jsonPath("$.[*].date").value(hasItem(DEFAULT_DATE.toString())))
            .andExpect(jsonPath("$.[*].entry").value(hasItem(DEFAULT_ENTRY)))
            .andExpect(jsonPath("$.[*].mood").value(hasItem(DEFAULT_MOOD.toString())));
    }

    @Test
    @Transactional
    void getGratitudeEntriesByDateRange() throws Exception {
        // Initialize the database
        gratitudeEntry = gratitudeEntryRepository.saveAndFlush(gratitudeEntry);

        // Create another entry for different date
        GratitudeEntry secondEntry = createEntity();
        secondEntry.setDate(DEFAULT_DATE.plusDays(1));
        secondEntry.setEntry("Second entry");
        secondEntry.setUser(testUser);
        gratitudeEntryRepository.saveAndFlush(secondEntry);

        // Get entries by date range
        restGratitudeEntryMockMvc
            .perform(
                get(ENTITY_API_URL + "/by-date-range")
                    .param("startDate", DEFAULT_DATE.toString())
                    .param("endDate", DEFAULT_DATE.plusDays(2).toString())
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @Transactional
    void getGratitudeEntryByDate() throws Exception {
        // Initialize the database
        gratitudeEntry = gratitudeEntryRepository.saveAndFlush(gratitudeEntry);

        // Get entry by date
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL + "/by-date/" + DEFAULT_DATE.toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(gratitudeEntry.getId().intValue()))
            .andExpect(jsonPath("$.date").value(DEFAULT_DATE.toString()))
            .andExpect(jsonPath("$.entry").value(DEFAULT_ENTRY));
    }

    @Test
    @Transactional
    void getTodaysGratitudeEntry() throws Exception {
        // Create entry for today
        GratitudeEntry todayEntry = createEntity();
        todayEntry.setDate(LocalDate.now());
        todayEntry.setUser(testUser);
        gratitudeEntryRepository.saveAndFlush(todayEntry);

        // Get today's entry
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL + "/today"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.date").value(LocalDate.now().toString()));
    }

    @Test
    @Transactional
    void getGratitudeEntry() throws Exception {
        // Initialize the database
        gratitudeEntry = gratitudeEntryRepository.saveAndFlush(gratitudeEntry);

        // Get the gratitudeEntry
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL_ID, gratitudeEntry.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(gratitudeEntry.getId().intValue()))
            .andExpect(jsonPath("$.date").value(DEFAULT_DATE.toString()))
            .andExpect(jsonPath("$.entry").value(DEFAULT_ENTRY))
            .andExpect(jsonPath("$.mood").value(DEFAULT_MOOD.toString()));
    }

    @Test
    @Transactional
    void updateGratitudeEntry() throws Exception {
        // Initialize the database
        gratitudeEntry = gratitudeEntryRepository.saveAndFlush(gratitudeEntry);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the gratitudeEntry
        GratitudeEntry updatedGratitudeEntry = gratitudeEntryRepository.findById(gratitudeEntry.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedGratitudeEntry are not directly saved in db
        em.detach(updatedGratitudeEntry);
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
        List<GratitudeEntry> gratitudeEntryList = gratitudeEntryRepository.findAll();
        GratitudeEntry testGratitudeEntry = gratitudeEntryList
            .stream()
            .filter(entry -> entry.getId().equals(gratitudeEntry.getId()))
            .findFirst()
            .orElseThrow();

        assertThat(testGratitudeEntry.getDate()).isEqualTo(UPDATED_DATE);
        assertThat(testGratitudeEntry.getEntry()).isEqualTo(UPDATED_ENTRY);
        assertThat(testGratitudeEntry.getMood()).isEqualTo(UPDATED_MOOD);
        // Timestamp should be preserved from original
        assertThat(testGratitudeEntry.getTimestamp()).isEqualTo(DEFAULT_TIMESTAMP);
    }

    @Test
    @Transactional
    void deleteGratitudeEntry() throws Exception {
        // Initialize the database
        gratitudeEntry = gratitudeEntryRepository.saveAndFlush(gratitudeEntry);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the gratitudeEntry
        restGratitudeEntryMockMvc
            .perform(delete(ENTITY_API_URL_ID, gratitudeEntry.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    @Test
    @Transactional
    void getNonExistingGratitudeEntry() throws Exception {
        // Get the gratitudeEntry
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
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
}
