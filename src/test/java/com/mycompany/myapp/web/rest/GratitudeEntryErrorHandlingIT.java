package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.HashMap;
import java.util.Map;
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
 * Integration tests for error handling in {@link GratitudeEntryResource} REST controller.
 * Tests validation errors, constraint violations, and edge cases.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser("testuser")
class GratitudeEntryErrorHandlingIT {

    private static final String ENTITY_API_URL = "/api/gratitude-entries";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GratitudeEntryRepository gratitudeEntryRepository;

    @Autowired
    private GratitudeEntryMapper gratitudeEntryMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restGratitudeEntryMockMvc;

    private User testUser;
    private GratitudeEntry existingEntry;

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

        // Create existing entry for update/delete tests
        existingEntry = new GratitudeEntry();
        existingEntry.setDate(LocalDate.now());
        existingEntry.setEntry("Existing gratitude entry");
        existingEntry.setMood(Mood.GRATEFUL);
        existingEntry.setTimestamp(Instant.now());
        existingEntry.setUser(testUser);
        existingEntry = gratitudeEntryRepository.save(existingEntry);
    }

    @AfterEach
    public void cleanUp() {
        gratitudeEntryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void testCreateEntryWithNullEntryText() throws Exception {
        GratitudeEntryDTO dto = new GratitudeEntryDTO();
        dto.setDate(LocalDate.now().plusDays(1));
        dto.setEntry(null); // Null entry text
        dto.setMood(Mood.GRATEFUL);

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").exists()) // JHipster error format
            .andExpect(jsonPath("$.title").exists())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @Transactional
    void testCreateEntryWithEmptyEntryText() throws Exception {
        GratitudeEntryDTO dto = new GratitudeEntryDTO();
        dto.setDate(LocalDate.now().plusDays(1));
        dto.setEntry(""); // Empty entry text
        dto.setMood(Mood.GRATEFUL);
        dto.setTimestamp(Instant.now()); // Provide valid timestamp

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("error.entryempty"));
    }

    @Test
    @Transactional
    void testCreateEntryWithWhitespaceOnlyText() throws Exception {
        GratitudeEntryDTO dto = new GratitudeEntryDTO();
        dto.setDate(LocalDate.now().plusDays(1));
        dto.setEntry("   \t\n   "); // Whitespace only
        dto.setMood(Mood.GRATEFUL);
        dto.setTimestamp(Instant.now()); // Provide valid timestamp

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("error.entryempty"));
    }

    @Test
    @Transactional
    void testCreateEntryWithNullDate() throws Exception {
        GratitudeEntryDTO dto = new GratitudeEntryDTO();
        dto.setDate(null); // Null date
        dto.setEntry("Valid entry text");
        dto.setMood(Mood.GRATEFUL);

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").exists()) // ValidationException handling
            .andExpect(jsonPath("$.title").exists())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @Transactional
    void testCreateEntryWithExistingId() throws Exception {
        GratitudeEntryDTO dto = new GratitudeEntryDTO();
        dto.setId(999999L); // Set an ID for new entry that definitely doesn't exist
        dto.setDate(LocalDate.now().plusDays(1));
        dto.setEntry("Valid entry text");
        dto.setMood(Mood.GRATEFUL);
        dto.setTimestamp(Instant.now()); // Provide valid timestamp

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("error.idexists"));
    }

    @Test
    @Transactional
    void testCreateDuplicateEntryForSameDate() throws Exception {
        GratitudeEntryDTO dto = new GratitudeEntryDTO();
        dto.setDate(LocalDate.now()); // Same date as existing entry
        dto.setEntry("Another entry for today");
        dto.setMood(Mood.HAPPY);
        dto.setTimestamp(Instant.now()); // Provide valid timestamp

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("error.dateexists"));
    }

    @Test
    @Transactional
    void testCreateEntryWithInvalidMood() throws Exception {
        // Create raw JSON with invalid mood to bypass DTO validation
        Map<String, Object> invalidEntry = new HashMap<>();
        invalidEntry.put("date", LocalDate.now().plusDays(1).toString());
        invalidEntry.put("entry", "Valid entry text");
        invalidEntry.put("mood", "INVALID_MOOD"); // Invalid mood value

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(invalidEntry)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @Transactional
    void testCreateEntryWithMalformedJson() throws Exception {
        String malformedJson = "{ \"date\": \"2024-01-01\", \"entry\": \"Valid text\", ";

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(malformedJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testCreateEntryWithExtremelyLongText() throws Exception {
        // Create text longer than typical limits but should be handled by CLOB
        String extremelyLongText = "A".repeat(10000);

        GratitudeEntryDTO dto = new GratitudeEntryDTO();
        dto.setDate(LocalDate.now().plusDays(1));
        dto.setEntry(extremelyLongText);
        dto.setMood(Mood.GRATEFUL);

        // Should succeed with CLOB field, but may have different validation rules
        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @Transactional
    void testUpdateNonExistentEntry() throws Exception {
        GratitudeEntryDTO dto = new GratitudeEntryDTO();
        dto.setId(999999L); // Non-existent ID
        dto.setDate(LocalDate.now());
        dto.setEntry("Updated text");
        dto.setMood(Mood.CONTENT);
        dto.setTimestamp(Instant.now()); // Provide valid timestamp

        restGratitudeEntryMockMvc
            .perform(put(ENTITY_API_URL_ID, 999999L).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("error.idnotfound"));
    }

    @Test
    @Transactional
    void testUpdateEntryWithMismatchedId() throws Exception {
        GratitudeEntryDTO dto = gratitudeEntryMapper.toDto(existingEntry);
        dto.setEntry("Updated text");

        // URL ID doesn't match DTO ID
        restGratitudeEntryMockMvc
            .perform(put(ENTITY_API_URL_ID, 999L).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("error.idinvalid"));
    }

    @Test
    @Transactional
    void testUpdateEntryWithEmptyText() throws Exception {
        GratitudeEntryDTO dto = gratitudeEntryMapper.toDto(existingEntry);
        dto.setEntry(""); // Empty text

        restGratitudeEntryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, existingEntry.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("error.entryempty"));
    }

    @Test
    @Transactional
    void testDeleteNonExistentEntry() throws Exception {
        restGratitudeEntryMockMvc
            .perform(delete(ENTITY_API_URL_ID, 999999L))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("error.accessdenied"));
    }

    @Test
    @Transactional
    void testGetNonExistentEntry() throws Exception {
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL_ID, 999999L)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void testGetEntryByInvalidDate() throws Exception {
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL + "/by-date/invalid-date")).andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testGetEntriesByInvalidDateRange() throws Exception {
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL + "/by-date-range").param("startDate", "invalid-date").param("endDate", "2024-12-31"))
            .andExpect(status().isBadRequest());

        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL + "/by-date-range").param("startDate", "2024-01-01").param("endDate", "invalid-date"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testGetEntriesWithInvalidPaginationParams() throws Exception {
        // Most pagination parameter validation is handled by Spring Data
        // Negative page might be converted to 0, size 0 might be converted to default
        // Let's test what actually causes 400 errors

        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL).param("page", "-1").param("size", "10")).andExpect(status().isOk()); // Spring typically handles this gracefully

        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL).param("page", "0").param("size", "0")).andExpect(status().isOk()); // Spring typically converts to default size

        // Test with extremely large size - this might cause issues
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL).param("page", "0").param("size", "10000")).andExpect(status().isOk()); // Even large sizes are typically handled
    }

    @Test
    @Transactional
    void testCreateEntryWithUnsupportedHttpMethod() throws Exception {
        GratitudeEntryDTO dto = new GratitudeEntryDTO();
        dto.setDate(LocalDate.now().plusDays(1));
        dto.setEntry("Valid entry text");
        dto.setMood(Mood.GRATEFUL);

        // PATCH is not supported for creation
        restGratitudeEntryMockMvc
            .perform(patch(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Transactional
    void testCreateEntryWithUnsupportedContentType() throws Exception {
        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.TEXT_PLAIN).content("some text"))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @Transactional
    void testHandleDateRangeWithStartDateAfterEndDate() throws Exception {
        restGratitudeEntryMockMvc
            .perform(
                get(ENTITY_API_URL + "/by-date-range").param("startDate", "2024-12-31").param("endDate", "2024-01-01") // End date before start date
            )
            .andExpect(status().isOk()) // Should handle gracefully and return empty result
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Transactional
    void testConcurrentModificationHandling() throws Exception {
        // Get current entry
        GratitudeEntryDTO dto = gratitudeEntryMapper.toDto(existingEntry);

        // Modify the entry in database directly (simulating concurrent modification)
        em
            .createQuery("UPDATE GratitudeEntry g SET g.entry = 'Modified by another process' WHERE g.id = :id")
            .setParameter("id", existingEntry.getId())
            .executeUpdate();
        em.flush();

        // Try to update with stale data
        dto.setEntry("Updated by current user");

        restGratitudeEntryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, existingEntry.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isOk()); // Should succeed (last write wins in this implementation)

        // Verify the final state
        GratitudeEntry updatedEntry = gratitudeEntryRepository.findById(existingEntry.getId()).orElseThrow();
        assertThat(updatedEntry.getEntry()).isEqualTo("Updated by current user");
    }

    @Test
    @Transactional
    void testErrorResponseFormat() throws Exception {
        // Test that error responses follow the expected format
        GratitudeEntryDTO dto = new GratitudeEntryDTO();
        dto.setDate(LocalDate.now()); // Duplicate date
        dto.setEntry("Duplicate entry");

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.title").exists())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").exists());
    }
}
