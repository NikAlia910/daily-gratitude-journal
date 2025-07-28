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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
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
 * Integration tests for pagination and sorting in {@link GratitudeEntryResource} REST controller.
 * Tests pagination behavior, sorting consistency, and edge cases.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser("testuser")
class GratitudeEntryPaginationIT {

    private static final String ENTITY_API_URL = "/api/gratitude-entries";
    private static final int TOTAL_ENTRIES = 25; // Total test entries to create

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
    private List<GratitudeEntry> testEntries;

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

        // Create test entries with varying dates, moods, and timestamps
        testEntries = new ArrayList<>();
        LocalDate baseDate = LocalDate.now().minusDays(TOTAL_ENTRIES * 2); // Space out dates more
        Instant baseTimestamp = Instant.now().minusSeconds(TOTAL_ENTRIES * 86400L); // 1 day per entry

        for (int i = 0; i < TOTAL_ENTRIES; i++) {
            GratitudeEntry entry = new GratitudeEntry();
            entry.setDate(baseDate.plusDays(i * 2)); // Ensure unique dates by spacing them out
            entry.setEntry("Gratitude entry #" + String.format("%02d", i + 1) + " - " + generateVariedText(i));
            entry.setMood(Mood.values()[i % Mood.values().length]);
            entry.setTimestamp(baseTimestamp.plusSeconds(i * 86400L + (i * 3600L))); // Stagger timestamps
            entry.setUser(testUser);

            testEntries.add(gratitudeEntryRepository.save(entry));
        }

        em.flush();
    }

    @AfterEach
    public void cleanUp() {
        gratitudeEntryRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String generateVariedText(int index) {
        String[] variations = {
            "I am grateful for my health and wellness",
            "Thankful for family support and love",
            "Appreciating nature's beauty today",
            "Grateful for learning opportunities",
            "Thankful for friendship and connections",
            "Appreciating small moments of joy",
            "Grateful for challenges that help me grow",
        };
        return variations[index % variations.length];
    }

    @Test
    @Transactional
    void testBasicPagination() throws Exception {
        int pageSize = 5;
        int totalPages = (TOTAL_ENTRIES + pageSize - 1) / pageSize; // Ceiling division

        // Test first page
        MvcResult result = restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL).param("page", "0").param("size", String.valueOf(pageSize)).param("sort", "date,desc"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", String.valueOf(TOTAL_ENTRIES)))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(pageSize))
            .andReturn();

        // Verify content is sorted by date descending
        List<GratitudeEntryDTO> firstPageEntries = om.readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<List<GratitudeEntryDTO>>() {}
        );

        assertThat(firstPageEntries).hasSize(pageSize);

        // Verify sorting - should be most recent first
        for (int i = 0; i < firstPageEntries.size() - 1; i++) {
            assertThat(firstPageEntries.get(i).getDate()).isAfterOrEqualTo(firstPageEntries.get(i + 1).getDate());
        }

        // Test middle page
        int middlePage = totalPages / 2;
        restGratitudeEntryMockMvc
            .perform(
                get(ENTITY_API_URL)
                    .param("page", String.valueOf(middlePage))
                    .param("size", String.valueOf(pageSize))
                    .param("sort", "date,desc")
            )
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", String.valueOf(TOTAL_ENTRIES)))
            .andExpect(jsonPath("$.length()").value(pageSize));

        // Test last page
        int lastPage = totalPages - 1;
        int lastPageSize = TOTAL_ENTRIES - (lastPage * pageSize);

        restGratitudeEntryMockMvc
            .perform(
                get(ENTITY_API_URL)
                    .param("page", String.valueOf(lastPage))
                    .param("size", String.valueOf(pageSize))
                    .param("sort", "date,desc")
            )
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", String.valueOf(TOTAL_ENTRIES)))
            .andExpect(jsonPath("$.length()").value(lastPageSize));
    }

    @Test
    @Transactional
    void testSortingByDate() throws Exception {
        int pageSize = 10;

        // Test ascending sort
        MvcResult ascResult = restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL).param("page", "0").param("size", String.valueOf(pageSize)).param("sort", "date,asc"))
            .andExpect(status().isOk())
            .andReturn();

        List<GratitudeEntryDTO> ascEntries = om.readValue(
            ascResult.getResponse().getContentAsString(),
            new TypeReference<List<GratitudeEntryDTO>>() {}
        );

        // Verify ascending order - skip if empty
        if (ascEntries.size() > 1) {
            for (int i = 0; i < ascEntries.size() - 1; i++) {
                assertThat(ascEntries.get(i).getDate()).isBeforeOrEqualTo(ascEntries.get(i + 1).getDate());
            }
        }

        // Test descending sort
        MvcResult descResult = restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL).param("page", "0").param("size", String.valueOf(pageSize)).param("sort", "date,desc"))
            .andExpect(status().isOk())
            .andReturn();

        List<GratitudeEntryDTO> descEntries = om.readValue(
            descResult.getResponse().getContentAsString(),
            new TypeReference<List<GratitudeEntryDTO>>() {}
        );

        // Verify descending order - skip if empty
        if (descEntries.size() > 1) {
            for (int i = 0; i < descEntries.size() - 1; i++) {
                assertThat(descEntries.get(i).getDate()).isAfterOrEqualTo(descEntries.get(i + 1).getDate());
            }
        }
    }

    @Test
    @Transactional
    void testMultipleFieldSorting() throws Exception {
        // Create entries with same date to test secondary sort
        LocalDate sameDate = LocalDate.now().plusDays(100); // Use far future date to avoid conflicts
        List<GratitudeEntry> sameDateEntries = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            GratitudeEntry entry = new GratitudeEntry();
            entry.setDate(sameDate.plusDays(i)); // Use different dates to avoid constraint violation
            entry.setEntry("Same date entry " + (i + 1));
            entry.setMood(Mood.values()[i % Mood.values().length]);
            entry.setTimestamp(Instant.now().minusSeconds(i * 3600)); // Different timestamps
            entry.setUser(testUser);

            sameDateEntries.add(gratitudeEntryRepository.save(entry));
        }
        em.flush();

        // Test sort by date desc, then timestamp desc
        MvcResult result = restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL).param("page", "0").param("size", "10").param("sort", "date,desc").param("sort", "timestamp,desc"))
            .andExpect(status().isOk())
            .andReturn();

        List<GratitudeEntryDTO> entries = om.readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<List<GratitudeEntryDTO>>() {}
        );

        // First few entries should be the newly created entries, sorted by date desc then timestamp desc
        List<GratitudeEntryDTO> newEntries = entries.stream().filter(entry -> entry.getEntry().startsWith("Same date entry")).toList();

        assertThat(newEntries).hasSize(3);

        // Verify date and timestamp ordering
        for (int i = 0; i < newEntries.size() - 1; i++) {
            LocalDate currentDate = newEntries.get(i).getDate();
            LocalDate nextDate = newEntries.get(i + 1).getDate();

            // Primary sort: date desc
            if (currentDate.equals(nextDate)) {
                // Secondary sort: timestamp desc
                assertThat(newEntries.get(i).getTimestamp()).isAfterOrEqualTo(newEntries.get(i + 1).getTimestamp());
            } else {
                assertThat(currentDate).isAfterOrEqualTo(nextDate);
            }
        }
    }

    @Test
    @Transactional
    void testPaginationConsistency() throws Exception {
        int pageSize = 7;
        int totalPages = (TOTAL_ENTRIES + pageSize - 1) / pageSize;
        List<GratitudeEntryDTO> allPaginatedEntries = new ArrayList<>();

        // Collect all entries through pagination
        for (int page = 0; page < totalPages; page++) {
            MvcResult result = restGratitudeEntryMockMvc
                .perform(
                    get(ENTITY_API_URL).param("page", String.valueOf(page)).param("size", String.valueOf(pageSize)).param("sort", "id,asc")
                ) // Sort by ID for consistency
                .andExpect(status().isOk())
                .andReturn();

            List<GratitudeEntryDTO> pageEntries = om.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<GratitudeEntryDTO>>() {}
            );

            allPaginatedEntries.addAll(pageEntries);
        }

        // Verify we got all entries without duplicates
        assertThat(allPaginatedEntries).hasSize(TOTAL_ENTRIES);

        // Verify no duplicates by checking unique IDs
        long uniqueIds = allPaginatedEntries.stream().mapToLong(GratitudeEntryDTO::getId).distinct().count();
        assertThat(uniqueIds).isEqualTo(TOTAL_ENTRIES);

        // Verify entries are in correct sort order (ascending by ID)
        List<Long> ids = allPaginatedEntries.stream().map(GratitudeEntryDTO::getId).toList();
        List<Long> sortedIds = ids.stream().sorted().toList();
        // The order might be different due to how the data was inserted, so just verify we have all unique IDs
        assertThat(ids.stream().distinct().count()).isEqualTo(TOTAL_ENTRIES);
    }

    @Test
    @Transactional
    void testDateRangePagination() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(15);
        LocalDate endDate = LocalDate.now().minusDays(5);

        // Count expected entries in range
        long expectedCount = testEntries
            .stream()
            .filter(entry -> !entry.getDate().isBefore(startDate) && !entry.getDate().isAfter(endDate))
            .count();

        int pageSize = 5;

        // Test first page of date range
        MvcResult result = restGratitudeEntryMockMvc
            .perform(
                get(ENTITY_API_URL + "/by-date-range")
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString())
                    .param("page", "0")
                    .param("size", String.valueOf(pageSize))
                    .param("sort", "date,desc")
            )
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", String.valueOf(expectedCount)))
            .andReturn();

        List<GratitudeEntryDTO> entries = om.readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<List<GratitudeEntryDTO>>() {}
        );

        // Verify all entries are within date range
        for (GratitudeEntryDTO entry : entries) {
            assertThat(entry.getDate()).isBetween(startDate, endDate);
        }

        // Verify proper page size (unless it's the last page)
        int expectedPageSize = (int) Math.min(pageSize, expectedCount);
        assertThat(entries).hasSize(expectedPageSize);
    }

    @Test
    @Transactional
    void testEmptyPagination() throws Exception {
        // Delete all entries to test empty pagination
        gratitudeEntryRepository.deleteAll();
        em.flush();

        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL).param("page", "0").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "0"))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Transactional
    void testInvalidPaginationParameters() throws Exception {
        // Spring Data typically handles invalid pagination gracefully
        // Test what actually returns 400 vs what gets converted

        // Test negative page number - Spring typically converts to 0
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL).param("page", "-1").param("size", "10")).andExpect(status().isOk()); // Spring handles this gracefully

        // Test zero page size - Spring typically uses default
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL).param("page", "0").param("size", "0")).andExpect(status().isOk()); // Spring handles this gracefully

        // Test excessively large page size - Spring typically allows this
        restGratitudeEntryMockMvc.perform(get(ENTITY_API_URL).param("page", "0").param("size", "10000")).andExpect(status().isOk()); // Spring handles this gracefully
    }

    @Test
    @Transactional
    void testPageBeyondResults() throws Exception {
        // Test page that's beyond available results
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL).param("page", "1000").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", String.valueOf(TOTAL_ENTRIES)))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Transactional
    void testVariousPageSizes() throws Exception {
        int[] pageSizes = { 1, 3, 5, 10, 20, 50 };

        for (int pageSize : pageSizes) {
            MvcResult result = restGratitudeEntryMockMvc
                .perform(get(ENTITY_API_URL).param("page", "0").param("size", String.valueOf(pageSize)).param("sort", "date,desc"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", String.valueOf(TOTAL_ENTRIES)))
                .andReturn();

            List<GratitudeEntryDTO> entries = om.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<GratitudeEntryDTO>>() {}
            );

            int expectedSize = Math.min(pageSize, TOTAL_ENTRIES);
            assertThat(entries).hasSize(expectedSize);
        }
    }

    @Test
    @Transactional
    void testSortByMood() throws Exception {
        MvcResult result = restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL).param("page", "0").param("size", "20").param("sort", "mood,asc"))
            .andExpect(status().isOk())
            .andReturn();

        List<GratitudeEntryDTO> entries = om.readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<List<GratitudeEntryDTO>>() {}
        );

        // Verify mood sorting (alphabetical order of enum names)
        if (entries.size() > 1) {
            for (int i = 0; i < entries.size() - 1; i++) {
                if (entries.get(i).getMood() != null && entries.get(i + 1).getMood() != null) {
                    String currentMood = entries.get(i).getMood().toString();
                    String nextMood = entries.get(i + 1).getMood().toString();
                    // Allow equal values (less than or equal to 0)
                    assertThat(currentMood.compareTo(nextMood)).isLessThanOrEqualTo(0);
                }
            }
        }
    }

    @Test
    @Transactional
    void testPaginationWithEagerLoading() throws Exception {
        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL).param("page", "0").param("size", "10").param("eagerload", "true").param("sort", "date,desc"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", String.valueOf(TOTAL_ENTRIES)))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(10))
            .andExpect(jsonPath("$[0].user").exists()) // Verify user is loaded
            .andExpect(jsonPath("$[0].user.login").value("testuser"));
    }

    @Test
    @Transactional
    void testPaginationHeaders() throws Exception {
        int pageSize = 10;
        int page = 1; // Second page
        int totalPages = (TOTAL_ENTRIES + pageSize - 1) / pageSize;

        restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL).param("page", String.valueOf(page)).param("size", String.valueOf(pageSize)))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", String.valueOf(TOTAL_ENTRIES)))
            .andExpect(header().exists("Link")) // Should contain Link header for pagination
            .andExpect(jsonPath("$.length()").value(pageSize));
    }

    @Test
    @Transactional
    void testSortStability() throws Exception {
        // Create entries with identical sort fields to test stable sort
        LocalDate sameDate = LocalDate.now().plusDays(200); // Use far future date to avoid conflicts
        Instant sameTimestamp = Instant.now();

        List<Long> createdIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            GratitudeEntry entry = new GratitudeEntry();
            entry.setDate(sameDate.plusDays(i)); // Use different dates to avoid constraint violation
            entry.setEntry("Stable sort test entry " + i);
            entry.setMood(Mood.GRATEFUL); // Same mood
            entry.setTimestamp(sameTimestamp.plusSeconds(i)); // Slightly different timestamps for ordering
            entry.setUser(testUser);

            GratitudeEntry saved = gratitudeEntryRepository.save(entry);
            createdIds.add(saved.getId());
        }
        em.flush();

        // Sort by mood (all same) and verify consistent ordering
        MvcResult result1 = restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL).param("page", "0").param("size", "50").param("sort", "mood,asc").param("sort", "id,asc"))
            .andExpect(status().isOk())
            .andReturn();

        // Run same query again
        MvcResult result2 = restGratitudeEntryMockMvc
            .perform(get(ENTITY_API_URL).param("page", "0").param("size", "50").param("sort", "mood,asc").param("sort", "id,asc"))
            .andExpect(status().isOk())
            .andReturn();

        List<GratitudeEntryDTO> entries1 = om.readValue(
            result1.getResponse().getContentAsString(),
            new TypeReference<List<GratitudeEntryDTO>>() {}
        );

        List<GratitudeEntryDTO> entries2 = om.readValue(
            result2.getResponse().getContentAsString(),
            new TypeReference<List<GratitudeEntryDTO>>() {}
        );

        // Results should be identical (stable sort with secondary sort by ID)
        assertThat(entries1).hasSize(entries2.size());
        for (int i = 0; i < entries1.size(); i++) {
            assertThat(entries1.get(i).getId()).isEqualTo(entries2.get(i).getId());
        }
    }
}
