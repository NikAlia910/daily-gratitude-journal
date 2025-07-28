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
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.dto.GratitudeEntryDTO;
import com.mycompany.myapp.service.mapper.GratitudeEntryMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for security aspects of the {@link GratitudeEntryResource} REST controller.
 * Tests user isolation, authorization, and access control.
 */
@IntegrationTest
@AutoConfigureMockMvc
class GratitudeEntrySecurityIT {

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

    private User user1;
    private User user2;
    private User adminUser;
    private GratitudeEntry user1Entry;
    private GratitudeEntry user2Entry;

    @BeforeEach
    public void initTest() {
        // Create first test user
        user1 = new User();
        user1.setLogin("user1");
        user1.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user1.setEmail("user1@example.com");
        user1.setFirstName("User");
        user1.setLastName("One");
        user1.setActivated(true);
        user1.setLangKey("en");
        user1 = userRepository.save(user1);

        // Create second test user
        user2 = new User();
        user2.setLogin("user2");
        user2.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user2.setEmail("user2@example.com");
        user2.setFirstName("User");
        user2.setLastName("Two");
        user2.setActivated(true);
        user2.setLangKey("en");
        user2 = userRepository.save(user2);

        // Create admin user only if it doesn't exist
        adminUser = userRepository.findOneByLogin("admin").orElseGet(() -> null);
        if (adminUser == null) {
            adminUser = new User();
            adminUser.setLogin("admin");
            adminUser.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
            adminUser.setEmail("admin@example.com");
            adminUser.setFirstName("Admin");
            adminUser.setLastName("User");
            adminUser.setActivated(true);
            adminUser.setLangKey("en");
            adminUser = userRepository.save(adminUser);
        }

        // Create gratitude entries for each user
        user1Entry = new GratitudeEntry();
        user1Entry.setDate(LocalDate.now());
        user1Entry.setEntry("User 1's gratitude entry");
        user1Entry.setMood(Mood.GRATEFUL);
        user1Entry.setTimestamp(Instant.now());
        user1Entry.setUser(user1);
        user1Entry = gratitudeEntryRepository.save(user1Entry);

        user2Entry = new GratitudeEntry();
        user2Entry.setDate(LocalDate.now().minusDays(1));
        user2Entry.setEntry("User 2's gratitude entry");
        user2Entry.setMood(Mood.HAPPY);
        user2Entry.setTimestamp(Instant.now());
        user2Entry.setUser(user2);
        user2Entry = gratitudeEntryRepository.save(user2Entry);
    }

    @AfterEach
    public void cleanUp() {
        gratitudeEntryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testUnauthenticatedUserCannotAccessGratitudeEntries() throws Exception {
        // Test that unauthenticated users get 401 for all endpoints
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL)).andExpect(status().isUnauthorized());

        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL_ID, user1Entry.getId())).andExpect(status().isUnauthorized());

        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL + "/today")).andExpect(status().isUnauthorized());

        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL + "/by-date-range")).andExpect(status().isUnauthorized());

        GratitudeEntryDTO dto = gratitudeEntryMapper.toDto(user1Entry);
        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isUnauthorized());

        restGratitudeEntryMockMvc
            .perform(put(ENTITY_API_URL_ID, user1Entry.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isUnauthorized());

        restGratitudeEntryMockMvc.perform(delete(ENTITY_API_URL_ID, user1Entry.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user1", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testUserCanOnlyAccessOwnEntries() throws Exception {
        // User 1 should see only their own entries
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(user1Entry.getId().intValue()))
            .andExpect(jsonPath("$[0].entry").value("User 1's gratitude entry"));
    }

    @Test
    @WithMockUser(username = "user1", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testUserCanAccessOwnEntryById() throws Exception {
        // User 1 should be able to access their own entry
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL_ID, user1Entry.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(user1Entry.getId().intValue()))
            .andExpect(jsonPath("$.entry").value("User 1's gratitude entry"));
    }

    @Test
    @WithMockUser(username = "user1", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testUserCannotAccessOtherUserEntry() throws Exception {
        // User 1 should not be able to access User 2's entry
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL_ID, user2Entry.getId())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testUserCannotUpdateOtherUserEntry() throws Exception {
        // User 1 should not be able to update User 2's entry
        GratitudeEntryDTO dto = gratitudeEntryMapper.toDto(user2Entry);
        dto.setEntry("Attempted unauthorized update");
        dto.setTimestamp(user2Entry.getTimestamp()); // Include original timestamp

        // The service layer should prevent this through security checks
        // This might return 404 (not found) or 400 (bad request) depending on implementation
        restGratitudeEntryMockMvc
            .perform(put(ENTITY_API_URL_ID, user2Entry.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest()); // Service layer should return bad request for unauthorized access
    }

    @Test
    @WithMockUser(username = "user1", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testUserCannotDeleteOtherUserEntry() throws Exception {
        // User 1 should not be able to delete User 2's entry
        restGratitudeEntryMockMvc.perform(delete(ENTITY_API_URL_ID, user2Entry.getId())).andExpect(status().isBadRequest());

        // Verify User 2's entry still exists
        assertThat(gratitudeEntryRepository.findById(user2Entry.getId())).isPresent();
    }

    @Test
    @WithMockUser(username = "user1", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testUserCanCreateOwnEntry() throws Exception {
        GratitudeEntryDTO dto = new GratitudeEntryDTO();
        dto.setDate(LocalDate.now().plusDays(10)); // Use future date to avoid conflicts
        dto.setEntry("New gratitude entry");
        dto.setMood(Mood.CONTENT);
        dto.setTimestamp(Instant.now()); // Explicitly set timestamp

        restGratitudeEntryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.entry").value("New gratitude entry"))
            .andExpect(jsonPath("$.user.login").value("user1"));
    }

    @Test
    @WithMockUser(username = "user1", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testUserCanUpdateOwnEntry() throws Exception {
        GratitudeEntryDTO dto = gratitudeEntryMapper.toDto(user1Entry);
        dto.setEntry("Updated gratitude entry");
        dto.setMood(Mood.REFLECTIVE);

        restGratitudeEntryMockMvc
            .perform(put(ENTITY_API_URL_ID, user1Entry.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entry").value("Updated gratitude entry"))
            .andExpect(jsonPath("$.mood").value("REFLECTIVE"));
    }

    @Test
    @WithMockUser(username = "user1", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testUserCanDeleteOwnEntry() throws Exception {
        restGratitudeEntryMockMvc.perform(delete(ENTITY_API_URL_ID, user1Entry.getId())).andExpect(status().isNoContent());

        // Verify entry is deleted
        assertThat(gratitudeEntryRepository.findById(user1Entry.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = "user2", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testDifferentUserSeesOnlyTheirEntries() throws Exception {
        // User 2 should see only their own entries
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(user2Entry.getId().intValue()))
            .andExpect(jsonPath("$[0].entry").value("User 2's gratitude entry"));
    }

    @Test
    @WithMockUser(username = "user1", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testDateRangeQueryRespectUserIsolation() throws Exception {
        // Create additional entries for both users
        GratitudeEntry user1Entry2 = new GratitudeEntry();
        user1Entry2.setDate(LocalDate.now().minusDays(2));
        user1Entry2.setEntry("User 1's second entry");
        user1Entry2.setMood(Mood.HOPEFUL);
        user1Entry2.setTimestamp(Instant.now());
        user1Entry2.setUser(user1);
        gratitudeEntryRepository.save(user1Entry2);

        // Query should return only User 1's entries
        restGratitudeEntryMockMvc
            .perform(
                get(ENTITY_API_URL + "/by-date-range")
                    .param("startDate", LocalDate.now().minusDays(5).toString())
                    .param("endDate", LocalDate.now().toString())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].user.login").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("user1"))));
    }

    @Test
    @WithMockUser(username = "user1", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testTodaysEntryQueryRespectUserIsolation() throws Exception {
        // Only User 1 has an entry for today, User 2's entry is for yesterday
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL + "/today"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user1Entry.getId().intValue()))
            .andExpect(jsonPath("$.user.login").value("user1"));
    }

    @Test
    @WithMockUser(username = "user2", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testTodaysEntryQueryReturnsNotFoundWhenNoEntry() throws Exception {
        // User 2 has no entry for today, should return 404
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL + "/today")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testSpecificDateQueryRespectUserIsolation() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // User 1 should not see User 2's entry from yesterday
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL + "/by-date/" + yesterday.toString())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "nonexistentuser", authorities = AuthoritiesConstants.USER)
    @Transactional
    void testNonExistentUserGetEmptyResults() throws Exception {
        // A user that doesn't exist in database should get empty results
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
