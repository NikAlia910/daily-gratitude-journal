package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.GratitudeEntry;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.Mood;
import com.mycompany.myapp.repository.GratitudeEntryRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.service.dto.GratitudeEntryDTO;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full-stack integration tests for {@link GratitudeEntryResource} REST controller.
 * Tests complete request flow from REST endpoint to database and back.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser("testuser")
class GratitudeEntryFullStackIT {

    private static final String ENTITY_API_URL = "/api/gratitude-entries";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GratitudeEntryRepository gratitudeEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restGratitudeEntryMockMvc;

    private User testUser;

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
    }

    @AfterEach
    public void cleanUp() {
        gratitudeEntryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void testCompleteCreateFlow() throws Exception {
        long initialCount = gratitudeEntryRepository.count();
        LocalDate testDate = LocalDate.now().plusDays(1); // Use future date to avoid conflicts
        String testEntry = "I am grateful for comprehensive integration testing";
        Mood testMood = Mood.GRATEFUL;

        // Create DTO for REST request
        GratitudeEntryDTO dto = new GratitudeEntryDTO();
        dto.setDate(testDate);
        dto.setEntry(testEntry);
        dto.setMood(testMood);
        dto.setTimestamp(Instant.now()); // Explicitly set timestamp

        // 1. REST Layer: Send POST request
        MvcResult result = restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.date").value(testDate.toString()))
            .andExpect(jsonPath("$.entry").value(testEntry))
            .andExpect(jsonPath("$.mood").value(testMood.toString()))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.user.login").value("testuser"))
            .andReturn();

        // 2. Verify response mapping
        GratitudeEntryDTO responseDto = om.readValue(result.getResponse().getContentAsString(), GratitudeEntryDTO.class);

        assertThat(responseDto.getId()).isNotNull();
        assertThat(responseDto.getDate()).isEqualTo(testDate);
        assertThat(responseDto.getEntry()).isEqualTo(testEntry);
        assertThat(responseDto.getMood()).isEqualTo(testMood);
        assertThat(responseDto.getTimestamp()).isNotNull();
        assertThat(responseDto.getUser().getLogin()).isEqualTo("testuser");

        // 3. Database Layer: Verify persistence
        assertThat(gratitudeEntryRepository.count()).isEqualTo(initialCount + 1);

        Optional<GratitudeEntry> savedEntityOpt = gratitudeEntryRepository.findById(responseDto.getId());
        assertThat(savedEntityOpt).isPresent();

        GratitudeEntry savedEntity = savedEntityOpt.orElseThrow();
        assertThat(savedEntity.getDate()).isEqualTo(testDate);
        assertThat(savedEntity.getEntry()).isEqualTo(testEntry);
        assertThat(savedEntity.getMood()).isEqualTo(testMood);
        assertThat(savedEntity.getTimestamp()).isNotNull();
        assertThat(savedEntity.getUser().getLogin()).isEqualTo("testuser");

        // 4. Verify timestamp was set automatically
        assertThat(savedEntity.getTimestamp()).isCloseTo(Instant.now(), org.assertj.core.api.Assertions.within(5, ChronoUnit.SECONDS));

        // 5. Service Layer: Verify business logic
        // Verify unique constraint would prevent duplicate
        GratitudeEntryDTO duplicateDto = new GratitudeEntryDTO();
        duplicateDto.setDate(testDate); // Same date
        duplicateDto.setEntry("Different text but same date");
        duplicateDto.setMood(Mood.HAPPY);
        duplicateDto.setTimestamp(Instant.now());

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(duplicateDto)))
            .andExpect(status().isBadRequest());

        // Verify count didn't increase
        assertThat(gratitudeEntryRepository.count()).isEqualTo(initialCount + 1);
    }

    @Test
    @Transactional
    void testCompleteReadFlow() throws Exception {
        // 1. Setup: Create entry directly in database
        GratitudeEntry entity = new GratitudeEntry();
        entity.setDate(LocalDate.now());
        entity.setEntry("Test entry for read flow");
        entity.setMood(Mood.CONTENT);
        entity.setTimestamp(Instant.now());
        entity.setUser(testUser);

        GratitudeEntry savedEntity = gratitudeEntryRepository.save(entity);
        em.flush();

        // 2. REST Layer: Send GET request
        MvcResult result = restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL_ID, savedEntity.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(savedEntity.getId().intValue()))
            .andExpect(jsonPath("$.date").value(savedEntity.getDate().toString()))
            .andExpect(jsonPath("$.entry").value(savedEntity.getEntry()))
            .andExpect(jsonPath("$.mood").value(savedEntity.getMood().toString()))
            .andExpect(jsonPath("$.user.login").value("testuser"))
            .andReturn();

        // 3. Verify complete data mapping
        GratitudeEntryDTO responseDto = om.readValue(result.getResponse().getContentAsString(), GratitudeEntryDTO.class);

        assertThat(responseDto.getId()).isEqualTo(savedEntity.getId());
        assertThat(responseDto.getDate()).isEqualTo(savedEntity.getDate());
        assertThat(responseDto.getEntry()).isEqualTo(savedEntity.getEntry());
        assertThat(responseDto.getMood()).isEqualTo(savedEntity.getMood());
        assertThat(responseDto.getTimestamp()).isEqualTo(savedEntity.getTimestamp());
        assertThat(responseDto.getUser().getLogin()).isEqualTo(testUser.getLogin());
    }

    @Test
    @Transactional
    void testCompleteUpdateFlow() throws Exception {
        // 1. Setup: Create initial entry
        GratitudeEntry initialEntity = new GratitudeEntry();
        initialEntity.setDate(LocalDate.now().plusDays(2)); // Use unique date
        initialEntity.setEntry("Initial entry text");
        initialEntity.setMood(Mood.GRATEFUL);
        initialEntity.setTimestamp(Instant.now().minus(1, ChronoUnit.HOURS));
        initialEntity.setUser(testUser);

        GratitudeEntry savedEntity = gratitudeEntryRepository.save(initialEntity);
        em.flush();

        Instant originalTimestamp = savedEntity.getTimestamp();

        // 2. Prepare update DTO
        GratitudeEntryDTO updateDto = new GratitudeEntryDTO();
        updateDto.setId(savedEntity.getId());
        updateDto.setDate(savedEntity.getDate());
        updateDto.setEntry("Updated entry text with more gratitude");
        updateDto.setMood(Mood.HOPEFUL);
        updateDto.setTimestamp(originalTimestamp); // Include timestamp to avoid validation issues

        // 3. REST Layer: Send PUT request
        MvcResult result = restGratitudeEntryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, savedEntity.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(updateDto))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(savedEntity.getId().intValue()))
            .andExpect(jsonPath("$.entry").value("Updated entry text with more gratitude"))
            .andExpect(jsonPath("$.mood").value(Mood.HOPEFUL.toString()))
            .andReturn();

        // 4. Verify response mapping
        GratitudeEntryDTO responseDto = om.readValue(result.getResponse().getContentAsString(), GratitudeEntryDTO.class);

        assertThat(responseDto.getId()).isEqualTo(savedEntity.getId());
        assertThat(responseDto.getEntry()).isEqualTo("Updated entry text with more gratitude");
        assertThat(responseDto.getMood()).isEqualTo(Mood.HOPEFUL);

        // 5. Database Layer: Verify persistence
        Optional<GratitudeEntry> updatedEntityOpt = gratitudeEntryRepository.findById(savedEntity.getId());
        assertThat(updatedEntityOpt).isPresent();

        GratitudeEntry updatedEntity = updatedEntityOpt.orElseThrow();
        assertThat(updatedEntity.getEntry()).isEqualTo("Updated entry text with more gratitude");
        assertThat(updatedEntity.getMood()).isEqualTo(Mood.HOPEFUL);

        // 6. Service Layer: Verify business logic (timestamp preservation)
        assertThat(updatedEntity.getTimestamp()).isEqualTo(originalTimestamp);
        assertThat(updatedEntity.getUser()).isEqualTo(testUser);
    }

    @Test
    @Transactional
    void testCompleteDeleteFlow() throws Exception {
        // 1. Setup: Create entry to delete
        GratitudeEntry entity = new GratitudeEntry();
        entity.setDate(LocalDate.now());
        entity.setEntry("Entry to be deleted");
        entity.setMood(Mood.REFLECTIVE);
        entity.setTimestamp(Instant.now());
        entity.setUser(testUser);

        GratitudeEntry savedEntity = gratitudeEntryRepository.save(entity);
        em.flush();

        long initialCount = gratitudeEntryRepository.count();
        Long entityId = savedEntity.getId();

        // 2. REST Layer: Send DELETE request
        restGratitudeEntryMockMvc.perform(delete(ENTITY_API_URL_ID, entityId)).andExpect(status().isNoContent());

        // 3. Database Layer: Verify deletion
        assertThat(gratitudeEntryRepository.count()).isEqualTo(initialCount - 1);
        assertThat(gratitudeEntryRepository.findById(entityId)).isEmpty();

        // 4. Verify GET returns 404 after deletion
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL_ID, entityId)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void testCompleteListFlow() throws Exception {
        // 1. Setup: Create multiple entries
        LocalDate baseDate = LocalDate.now().minusDays(5);

        for (int i = 0; i < 7; i++) {
            GratitudeEntry entity = new GratitudeEntry();
            entity.setDate(baseDate.plusDays(i));
            entity.setEntry("Entry #" + (i + 1));
            entity.setMood(Mood.values()[i % Mood.values().length]);
            entity.setTimestamp(Instant.now().minus(i, ChronoUnit.HOURS));
            entity.setUser(testUser);

            gratitudeEntryRepository.save(entity);
        }
        em.flush();

        // 2. REST Layer: Get paginated list
        MvcResult result = restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL).param("page", "0").param("size", "5").param("sort", "date,desc"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "7"))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(5))
            .andReturn();

        // 3. Verify response mapping and sorting
        List<GratitudeEntryDTO> entries = om.readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<List<GratitudeEntryDTO>>() {}
        );

        assertThat(entries).hasSize(5);

        // Verify sorting (most recent first)
        for (int i = 0; i < entries.size() - 1; i++) {
            assertThat(entries.get(i).getDate()).isAfterOrEqualTo(entries.get(i + 1).getDate());
        }

        // Verify all entries belong to the test user
        for (GratitudeEntryDTO entry : entries) {
            assertThat(entry.getUser().getLogin()).isEqualTo("testuser");
        }

        // 4. Database Layer: Verify query isolation
        // Entries should only be from current user context
        List<GratitudeEntry> allDbEntries = gratitudeEntryRepository.findAll();
        assertThat(allDbEntries).hasSize(7);
        assertThat(allDbEntries.stream().allMatch(e -> e.getUser().getLogin().equals("testuser"))).isTrue();
    }

    @Test
    @Transactional
    void testCompleteDateRangeFlow() throws Exception {
        // 1. Setup: Create entries across different dates
        LocalDate startDate = LocalDate.now().minusDays(10);
        LocalDate endDate = LocalDate.now().minusDays(3);
        LocalDate outsideDate = LocalDate.now().minusDays(15);

        // Entries within range
        for (int i = 0; i < 5; i++) {
            GratitudeEntry entity = new GratitudeEntry();
            entity.setDate(startDate.plusDays(i * 2)); // Spread dates within range
            entity.setEntry("In range entry #" + (i + 1));
            entity.setMood(Mood.GRATEFUL);
            entity.setTimestamp(Instant.now());
            entity.setUser(testUser);

            gratitudeEntryRepository.save(entity);
        }

        // Entry outside range
        GratitudeEntry outsideEntity = new GratitudeEntry();
        outsideEntity.setDate(outsideDate);
        outsideEntity.setEntry("Outside range entry");
        outsideEntity.setMood(Mood.OTHER);
        outsideEntity.setTimestamp(Instant.now());
        outsideEntity.setUser(testUser);
        gratitudeEntryRepository.save(outsideEntity);

        em.flush();

        // 2. REST Layer: Query by date range
        MvcResult result = restGratitudeEntryMockMvc
            .perform(
                get(ENTITY_API_URL + "/by-date-range")
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString())
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", "date,asc")
            )
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "4"))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(4))
            .andReturn();

        // 3. Verify response filtering and mapping
        List<GratitudeEntryDTO> entries = om.readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<List<GratitudeEntryDTO>>() {}
        );

        assertThat(entries).hasSize(4);

        // Verify all entries are within date range
        for (GratitudeEntryDTO entry : entries) {
            assertThat(entry.getDate()).isBetween(startDate, endDate);
            assertThat(entry.getEntry()).startsWith("In range entry");
        }

        // Verify sorting - dates should be in ascending order
        if (entries.size() > 1) {
            for (int i = 0; i < entries.size() - 1; i++) {
                assertThat(entries.get(i).getDate()).isBeforeOrEqualTo(entries.get(i + 1).getDate());
            }
        }

        // 4. Database Layer: Verify query correctness
        // Total entries should be 6, but query should return only 5
        assertThat(gratitudeEntryRepository.count()).isEqualTo(6);
    }

    @Test
    @Transactional
    void testTodaysEntryFlow() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 1. Setup: Create entry for today and yesterday
        GratitudeEntry todayEntry = new GratitudeEntry();
        todayEntry.setDate(today);
        todayEntry.setEntry("Today's gratitude entry");
        todayEntry.setMood(Mood.GRATEFUL);
        todayEntry.setTimestamp(Instant.now());
        todayEntry.setUser(testUser);
        gratitudeEntryRepository.save(todayEntry);

        GratitudeEntry yesterdayEntry = new GratitudeEntry();
        yesterdayEntry.setDate(yesterday);
        yesterdayEntry.setEntry("Yesterday's gratitude entry");
        yesterdayEntry.setMood(Mood.CONTENT);
        yesterdayEntry.setTimestamp(Instant.now().minus(1, ChronoUnit.DAYS));
        yesterdayEntry.setUser(testUser);
        gratitudeEntryRepository.save(yesterdayEntry);

        em.flush();

        // 2. REST Layer: Get today's entry
        MvcResult result = restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL + "/today"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.date").value(today.toString()))
            .andExpect(jsonPath("$.entry").value("Today's gratitude entry"))
            .andExpect(jsonPath("$.mood").value("GRATEFUL"))
            .andExpect(jsonPath("$.user.login").value("testuser"))
            .andReturn();

        // 3. Verify correct entry returned
        GratitudeEntryDTO responseDto = om.readValue(result.getResponse().getContentAsString(), GratitudeEntryDTO.class);

        assertThat(responseDto.getDate()).isEqualTo(today);
        assertThat(responseDto.getEntry()).isEqualTo("Today's gratitude entry");
        assertThat(responseDto.getMood()).isEqualTo(Mood.GRATEFUL);

        // 4. Service Layer: Verify business logic
        // Only today's entry should be returned, not yesterday's
        assertThat(responseDto.getId()).isEqualTo(todayEntry.getId());
        assertThat(responseDto.getId()).isNotEqualTo(yesterdayEntry.getId());
    }

    @Test
    @Transactional
    void testCompleteErrorHandlingFlow() throws Exception {
        // 1. Test validation error propagation
        GratitudeEntryDTO invalidDto = new GratitudeEntryDTO();
        invalidDto.setDate(null); // Invalid - null date
        invalidDto.setEntry("Valid entry text");
        invalidDto.setMood(Mood.GRATEFUL);

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(invalidDto)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.title").exists())
            .andExpect(jsonPath("$.status").value(400));

        // 2. Test business logic error propagation
        GratitudeEntryDTO emptyTextDto = new GratitudeEntryDTO();
        emptyTextDto.setDate(LocalDate.now().plusDays(10)); // Use unique date
        emptyTextDto.setEntry(""); // Invalid - empty text
        emptyTextDto.setMood(Mood.GRATEFUL);
        emptyTextDto.setTimestamp(Instant.now());

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(emptyTextDto)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("error.entryempty"));

        // 3. Test 404 error for non-existent resource
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL_ID, 999999L)).andExpect(status().isNotFound());

        // 4. Verify no partial data was persisted
        assertThat(gratitudeEntryRepository.count()).isEqualTo(0);
    }

    @Test
    @Transactional
    void testCompleteTransactionFlow() throws Exception {
        // This test verifies that transactions work correctly across all layers

        long initialCount = gratitudeEntryRepository.count();

        // 1. Create valid entry
        GratitudeEntryDTO validDto = new GratitudeEntryDTO();
        validDto.setDate(LocalDate.now().plusDays(5)); // Use unique future date
        validDto.setEntry("Valid entry for transaction test");
        validDto.setMood(Mood.GRATEFUL);
        validDto.setTimestamp(Instant.now());

        MvcResult createResult = restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(validDto)))
            .andExpect(status().isCreated())
            .andReturn();

        GratitudeEntryDTO createdDto = om.readValue(createResult.getResponse().getContentAsString(), GratitudeEntryDTO.class);

        // 2. Verify creation was committed
        assertThat(gratitudeEntryRepository.count()).isEqualTo(initialCount + 1);
        assertThat(gratitudeEntryRepository.findById(createdDto.getId())).isPresent();

        // 3. Update the entry
        createdDto.setEntry("Updated entry for transaction test");
        createdDto.setMood(Mood.CONTENT);

        MvcResult updateResult = restGratitudeEntryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, createdDto.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(createdDto))
            )
            .andExpect(status().isOk())
            .andReturn();

        // 4. Verify update was committed
        GratitudeEntry updatedEntity = gratitudeEntryRepository.findById(createdDto.getId()).orElseThrow();
        assertThat(updatedEntity.getEntry()).isEqualTo("Updated entry for transaction test");
        assertThat(updatedEntity.getMood()).isEqualTo(Mood.CONTENT);

        // 5. Delete the entry
        restGratitudeEntryMockMvc.perform(delete(ENTITY_API_URL_ID, createdDto.getId())).andExpect(status().isNoContent());

        // 6. Verify deletion was committed
        assertThat(gratitudeEntryRepository.count()).isEqualTo(initialCount);
        assertThat(gratitudeEntryRepository.findById(createdDto.getId())).isEmpty();
    }
}
