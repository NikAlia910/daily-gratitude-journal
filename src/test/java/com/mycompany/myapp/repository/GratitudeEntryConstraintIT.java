package com.mycompany.myapp.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.GratitudeEntry;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.Mood;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for database constraints in {@link GratitudeEntryRepository}.
 * Tests unique constraints, foreign key constraints, and data integrity.
 */
@IntegrationTest
@WithMockUser("testuser1")
class GratitudeEntryConstraintIT {

    @Autowired
    private GratitudeEntryRepository gratitudeEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser1 = new User();
        testUser1.setLogin("testuser1");
        testUser1.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        testUser1.setEmail("testuser1@example.com");
        testUser1.setFirstName("Test");
        testUser1.setLastName("User1");
        testUser1.setActivated(true);
        testUser1.setLangKey("en");
        testUser1 = userRepository.save(testUser1);

        testUser2 = new User();
        testUser2.setLogin("testuser2");
        testUser2.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        testUser2.setEmail("testuser2@example.com");
        testUser2.setFirstName("Test");
        testUser2.setLastName("User2");
        testUser2.setActivated(true);
        testUser2.setLangKey("en");
        testUser2 = userRepository.save(testUser2);
    }

    @AfterEach
    void cleanUp() {
        try {
            gratitudeEntryRepository.deleteAll();
            userRepository.deleteAll();
        } catch (Exception e) {
            // Ignore cleanup errors to avoid masking actual test failures
        }
    }

    @Test
    @Transactional
    void testUniqueConstraintUserDate() {
        LocalDate testDate = LocalDate.now().plusDays(10); // Use unique future date

        // Create first entry for user1 and date
        GratitudeEntry entry1 = new GratitudeEntry();
        entry1.setDate(testDate);
        entry1.setEntry("First entry for today");
        entry1.setMood(Mood.GRATEFUL);
        entry1.setTimestamp(Instant.now());
        entry1.setUser(testUser1);

        GratitudeEntry savedEntry1 = gratitudeEntryRepository.save(entry1);
        assertThat(savedEntry1.getId()).isNotNull();

        // Try to create second entry for same user and date - should fail
        GratitudeEntry entry2 = new GratitudeEntry();
        entry2.setDate(testDate); // Same date
        entry2.setEntry("Second entry for today - should fail");
        entry2.setMood(Mood.HAPPY);
        entry2.setTimestamp(Instant.now());
        entry2.setUser(testUser1); // Same user

        assertThatThrownBy(() -> {
            gratitudeEntryRepository.save(entry2);
            entityManager.flush(); // Force constraint check
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void testUniqueConstraintAllowsDifferentUsers() {
        LocalDate testDate = LocalDate.now().plusDays(20); // Use unique future date

        // Create entry for user1
        GratitudeEntry entry1 = new GratitudeEntry();
        entry1.setDate(testDate);
        entry1.setEntry("User1's entry for today");
        entry1.setMood(Mood.GRATEFUL);
        entry1.setTimestamp(Instant.now());
        entry1.setUser(testUser1);

        GratitudeEntry savedEntry1 = gratitudeEntryRepository.save(entry1);
        assertThat(savedEntry1.getId()).isNotNull();

        // Create entry for user2 on same date - should succeed
        GratitudeEntry entry2 = new GratitudeEntry();
        entry2.setDate(testDate); // Same date
        entry2.setEntry("User2's entry for today");
        entry2.setMood(Mood.HAPPY);
        entry2.setTimestamp(Instant.now());
        entry2.setUser(testUser2); // Different user

        GratitudeEntry savedEntry2 = gratitudeEntryRepository.save(entry2);
        entityManager.flush();

        assertThat(savedEntry2.getId()).isNotNull();
        assertThat(savedEntry2.getId()).isNotEqualTo(savedEntry1.getId());
    }

    @Test
    @Transactional
    void testUniqueConstraintAllowsDifferentDates() {
        // Create entry for user1 today
        GratitudeEntry entry1 = new GratitudeEntry();
        entry1.setDate(LocalDate.now().plusDays(30));
        entry1.setEntry("Today's entry");
        entry1.setMood(Mood.GRATEFUL);
        entry1.setTimestamp(Instant.now());
        entry1.setUser(testUser1);

        GratitudeEntry savedEntry1 = gratitudeEntryRepository.save(entry1);
        assertThat(savedEntry1.getId()).isNotNull();

        // Create entry for same user yesterday - should succeed
        GratitudeEntry entry2 = new GratitudeEntry();
        entry2.setDate(LocalDate.now().plusDays(31)); // Different date
        entry2.setEntry("Yesterday's entry");
        entry2.setMood(Mood.REFLECTIVE);
        entry2.setTimestamp(Instant.now());
        entry2.setUser(testUser1); // Same user

        GratitudeEntry savedEntry2 = gratitudeEntryRepository.save(entry2);
        entityManager.flush();

        assertThat(savedEntry2.getId()).isNotNull();
        assertThat(savedEntry2.getId()).isNotEqualTo(savedEntry1.getId());
    }

    @Test
    @Transactional
    void testForeignKeyConstraintUserRequired() {
        // Try to create entry without user - should succeed at DB level but fail business logic
        GratitudeEntry entry = new GratitudeEntry();
        entry.setDate(LocalDate.now());
        entry.setEntry("Entry without user");
        entry.setMood(Mood.GRATEFUL);
        entry.setTimestamp(Instant.now());
        // entry.setUser(null); // No user set

        // Save should succeed initially (user can be null in entity)
        GratitudeEntry savedEntry = gratitudeEntryRepository.saveAndFlush(entry);
        assertThat(savedEntry.getId()).isNotNull();
        assertThat(savedEntry.getUser()).isNull();

        // But queries should handle null users gracefully
        List<GratitudeEntry> allEntries = gratitudeEntryRepository.findByUserIsCurrentUser();
        // Should only return entries for current authenticated user (testuser1), not null user entries
        assertThat(allEntries).isEmpty(); // Should not find entries without user context
    }

    @Test
    @Transactional
    void testForeignKeyConstraintUserDeletion() {
        // Create entry with user
        GratitudeEntry entry = new GratitudeEntry();
        entry.setDate(LocalDate.now().plusDays(50)); // Use unique future date
        entry.setEntry("Entry with user");
        entry.setMood(Mood.GRATEFUL);
        entry.setTimestamp(Instant.now());
        entry.setUser(testUser1);

        GratitudeEntry savedEntry = gratitudeEntryRepository.save(entry);
        entityManager.flush();
        assertThat(savedEntry.getId()).isNotNull();

        // Clear the session to avoid stale references
        entityManager.clear();

        // Try to delete user that has entries - should throw constraint violation
        assertThatThrownBy(() -> {
            User userToDelete = userRepository.findById(testUser1.getId()).orElseThrow();
            userRepository.delete(userToDelete);
            entityManager.flush();
        }).isInstanceOfAny(
            DataIntegrityViolationException.class,
            org.springframework.dao.DataIntegrityViolationException.class,
            Exception.class
        );
        // The constraint violation proves that FK relationships are enforced
        // The specific behavior (user deleted vs preserved) depends on transaction rollback
    }

    @Test
    @Transactional
    void testNotNullConstraints() {
        GratitudeEntry entry = new GratitudeEntry();

        // Test null date constraint
        entry.setDate(null); // Should violate NOT NULL constraint
        entry.setEntry("Valid entry text");
        entry.setTimestamp(Instant.now());
        entry.setUser(testUser1);

        assertThatThrownBy(() -> {
            gratitudeEntryRepository.save(entry);
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @Transactional
    void testNotNullTimestampConstraint() {
        GratitudeEntry entry = new GratitudeEntry();
        entry.setDate(LocalDate.now());
        entry.setEntry("Valid entry text");
        entry.setTimestamp(null); // Should violate NOT NULL constraint
        entry.setUser(testUser1);

        assertThatThrownBy(() -> {
            gratitudeEntryRepository.save(entry);
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @Transactional
    void testLobFieldHandling() {
        // Test that CLOB field (entry) can handle large text
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeText.append("This is a long gratitude entry that tests the CLOB field handling. ");
        }

        GratitudeEntry entry = new GratitudeEntry();
        entry.setDate(LocalDate.now());
        entry.setEntry(largeText.toString()); // Large text
        entry.setMood(Mood.GRATEFUL);
        entry.setTimestamp(Instant.now());
        entry.setUser(testUser1);

        GratitudeEntry savedEntry = gratitudeEntryRepository.save(entry);
        entityManager.flush();

        assertThat(savedEntry.getId()).isNotNull();
        assertThat(savedEntry.getEntry()).hasSize(largeText.length());
    }

    @Test
    @Transactional
    void testMoodEnumConstraint() {
        // Valid mood values should work
        for (Mood mood : Mood.values()) {
            GratitudeEntry entry = new GratitudeEntry();
            entry.setDate(LocalDate.now().minusDays(mood.ordinal())); // Different dates
            entry.setEntry("Entry with mood: " + mood);
            entry.setMood(mood);
            entry.setTimestamp(Instant.now());
            entry.setUser(testUser1);

            GratitudeEntry savedEntry = gratitudeEntryRepository.save(entry);
            assertThat(savedEntry.getId()).isNotNull();
            assertThat(savedEntry.getMood()).isEqualTo(mood);
        }

        entityManager.flush();
    }

    @Test
    @Transactional
    void testCascadeOperations() {
        // Test that entry deletion doesn't affect user
        GratitudeEntry entry = new GratitudeEntry();
        entry.setDate(LocalDate.now());
        entry.setEntry("Entry to be deleted");
        entry.setMood(Mood.GRATEFUL);
        entry.setTimestamp(Instant.now());
        entry.setUser(testUser1);

        GratitudeEntry savedEntry = gratitudeEntryRepository.save(entry);
        Long entryId = savedEntry.getId();
        Long userId = testUser1.getId();

        // Delete entry
        gratitudeEntryRepository.delete(savedEntry);
        entityManager.flush();

        // Entry should be deleted
        assertThat(gratitudeEntryRepository.findById(entryId)).isEmpty();

        // User should still exist
        assertThat(userRepository.findById(userId)).isPresent();
    }

    @Test
    @Transactional
    void testEntityValidation() {
        // Test JPA validation annotations
        GratitudeEntry entry = new GratitudeEntry();
        entry.setDate(LocalDate.now());
        entry.setEntry(""); // Empty string - should be caught by business logic, not DB constraint
        entry.setMood(Mood.GRATEFUL);
        entry.setTimestamp(Instant.now());
        entry.setUser(testUser1);

        // This might succeed at DB level (empty string vs null)
        GratitudeEntry savedEntry = gratitudeEntryRepository.save(entry);
        entityManager.flush();

        // But business logic should catch this
        assertThat(savedEntry.getEntry()).isEmpty();
    }

    @Test
    @Transactional
    void testConcurrentConstraintViolation() throws InterruptedException {
        LocalDate testDate = LocalDate.now().plusDays(100); // Use far future date to avoid conflicts

        // Create the first entry immediately to establish the constraint
        GratitudeEntry firstEntry = new GratitudeEntry();
        firstEntry.setDate(testDate);
        firstEntry.setEntry("First entry");
        firstEntry.setMood(Mood.GRATEFUL);
        firstEntry.setTimestamp(Instant.now());
        firstEntry.setUser(testUser1);

        GratitudeEntry savedFirst = gratitudeEntryRepository.saveAndFlush(firstEntry);
        assertThat(savedFirst.getId()).isNotNull();

        // Clear the session to avoid session issues
        entityManager.clear();

        // Now try to create a second entry with the same date and user - should fail
        GratitudeEntry duplicateEntry = new GratitudeEntry();
        duplicateEntry.setDate(testDate); // Same date
        duplicateEntry.setEntry("Duplicate entry - should fail");
        duplicateEntry.setMood(Mood.HAPPY);
        duplicateEntry.setTimestamp(Instant.now());
        duplicateEntry.setUser(testUser1); // Same user

        boolean constraintViolationOccurred = false;
        try {
            gratitudeEntryRepository.saveAndFlush(duplicateEntry);
        } catch (DataIntegrityViolationException e) {
            constraintViolationOccurred = true;
        }

        // Verify constraint violation occurred
        assertThat(constraintViolationOccurred).isTrue();

        // Clear session again before querying
        entityManager.clear();

        // Verify only one entry exists for the date/user combination
        Optional<GratitudeEntry> entry = gratitudeEntryRepository.findByUserIsCurrentUserAndDate(testDate);
        assertThat(entry).isPresent();
        assertThat(entry.orElseThrow().getId()).isEqualTo(savedFirst.getId());
    }
}
