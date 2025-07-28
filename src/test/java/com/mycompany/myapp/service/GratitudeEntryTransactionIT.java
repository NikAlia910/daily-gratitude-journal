package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.GratitudeEntry;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.Mood;
import com.mycompany.myapp.repository.GratitudeEntryRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.dto.GratitudeEntryDTO;
import com.mycompany.myapp.service.mapper.GratitudeEntryMapper;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration tests for transaction behavior in {@link GratitudeEntryService}.
 * Tests transaction boundaries, rollback scenarios, and data consistency.
 */
@IntegrationTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GratitudeEntryTransactionIT {

    @Autowired
    private GratitudeEntryService gratitudeEntryService;

    @Autowired
    private GratitudeEntryRepository gratitudeEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GratitudeEntryMapper gratitudeEntryMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User testUser;
    private GratitudeEntryDTO gratitudeEntryDTO;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setLogin("testuser");
        testUser.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setActivated(true);
        testUser.setLangKey("en");
        testUser = userRepository.save(testUser);

        // Setup security context
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
        SecurityContextHolder.setContext(securityContext);

        // Create DTO for testing
        gratitudeEntryDTO = new GratitudeEntryDTO();
        gratitudeEntryDTO.setDate(LocalDate.now());
        gratitudeEntryDTO.setEntry("Test gratitude entry");
        gratitudeEntryDTO.setMood(Mood.GRATEFUL);
        gratitudeEntryDTO.setTimestamp(Instant.now());
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        gratitudeEntryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void testSaveWithinTransaction() {
        // Verify we're in a transaction
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();

        long initialCount = gratitudeEntryRepository.count();

        // Save entry
        GratitudeEntryDTO savedEntry = gratitudeEntryService.save(gratitudeEntryDTO);

        // Verify entry is saved
        assertThat(savedEntry.getId()).isNotNull();
        assertThat(gratitudeEntryRepository.count()).isEqualTo(initialCount + 1);

        // Verify the entity is in the persistence context
        entityManager.flush();
        entityManager.clear();

        Optional<GratitudeEntry> foundEntry = gratitudeEntryRepository.findById(savedEntry.getId());
        assertThat(foundEntry).isPresent();
        assertThat(foundEntry.orElseThrow().getEntry()).isEqualTo("Test gratitude entry");
    }

    @Test
    void testSaveFailureRollback() {
        long initialCount = gratitudeEntryRepository.count();

        // Test rollback when save fails
        assertThatThrownBy(() -> {
            transactionTemplate.execute(status -> {
                // Create entry with empty text to trigger validation error
                GratitudeEntryDTO invalidEntry = new GratitudeEntryDTO();
                invalidEntry.setDate(LocalDate.now());
                invalidEntry.setEntry(""); // This should trigger validation error
                invalidEntry.setMood(Mood.GRATEFUL);

                gratitudeEntryService.save(invalidEntry);
                return null;
            });
        }).isInstanceOf(BadRequestAlertException.class);

        // Verify no entry was saved due to rollback
        assertThat(gratitudeEntryRepository.count()).isEqualTo(initialCount);
    }

    @Test
    void testDuplicateDateConstraintRollback() {
        long initialCount = gratitudeEntryRepository.count();

        // First save should succeed
        transactionTemplate.execute(status -> {
            gratitudeEntryService.save(gratitudeEntryDTO);
            return null;
        });

        assertThat(gratitudeEntryRepository.count()).isEqualTo(initialCount + 1);

        // Second save with same date should fail and rollback
        assertThatThrownBy(() -> {
            transactionTemplate.execute(status -> {
                GratitudeEntryDTO duplicateEntry = new GratitudeEntryDTO();
                duplicateEntry.setDate(LocalDate.now()); // Same date as first entry
                duplicateEntry.setEntry("Duplicate entry for same date");
                duplicateEntry.setMood(Mood.HAPPY);

                gratitudeEntryService.save(duplicateEntry);
                return null;
            });
        }).isInstanceOf(BadRequestAlertException.class);

        // Verify count remains the same (only first entry saved)
        assertThat(gratitudeEntryRepository.count()).isEqualTo(initialCount + 1);
    }

    @Test
    void testUpdateTransactionConsistency() {
        // Create initial entry
        GratitudeEntryDTO savedEntry = transactionTemplate.execute(status -> {
            return gratitudeEntryService.save(gratitudeEntryDTO);
        });

        assertThat(savedEntry).isNotNull();
        Instant originalTimestamp = savedEntry.getTimestamp();

        // Update the entry
        transactionTemplate.execute(status -> {
            savedEntry.setEntry("Updated gratitude entry");
            savedEntry.setMood(Mood.CONTENT);

            GratitudeEntryDTO updatedEntry = gratitudeEntryService.update(savedEntry);

            // Verify timestamp is preserved during update (ignore precision differences)
            assertThat(updatedEntry.getTimestamp().toEpochMilli()).isEqualTo(originalTimestamp.toEpochMilli());

            return updatedEntry;
        });

        // Verify update is persisted
        Optional<GratitudeEntry> foundEntry = gratitudeEntryRepository.findById(savedEntry.getId());
        assertThat(foundEntry).isPresent();
        assertThat(foundEntry.orElseThrow().getEntry()).isEqualTo("Updated gratitude entry");
        assertThat(foundEntry.orElseThrow().getMood()).isEqualTo(Mood.CONTENT);
        // Verify timestamp is preserved (ignore precision differences)
        assertThat(foundEntry.orElseThrow().getTimestamp().toEpochMilli()).isEqualTo(originalTimestamp.toEpochMilli());
    }

    @Test
    void testDeleteTransactionConsistency() {
        // Create entry
        GratitudeEntryDTO savedEntry = transactionTemplate.execute(status -> {
            return gratitudeEntryService.save(gratitudeEntryDTO);
        });

        assertThat(savedEntry).isNotNull();
        assertThat(gratitudeEntryRepository.findById(savedEntry.getId())).isPresent();

        // Delete entry
        transactionTemplate.execute(status -> {
            gratitudeEntryService.delete(savedEntry.getId());
            return null;
        });

        // Verify entry is deleted
        assertThat(gratitudeEntryRepository.findById(savedEntry.getId())).isEmpty();
    }

    @Test
    void testDeleteFailureRollback() {
        // Create entry
        GratitudeEntryDTO savedEntry = transactionTemplate.execute(status -> {
            return gratitudeEntryService.save(gratitudeEntryDTO);
        });

        // Clear security context to simulate unauthorized access
        SecurityContextHolder.clearContext();

        // Attempt to delete should fail
        assertThatThrownBy(() -> {
            transactionTemplate.execute(status -> {
                gratitudeEntryService.delete(savedEntry.getId());
                return null;
            });
        }).isInstanceOf(BadRequestAlertException.class);

        // Restore security context
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
        SecurityContextHolder.setContext(securityContext);

        // Verify entry still exists (delete was rolled back)
        assertThat(gratitudeEntryRepository.findById(savedEntry.getId())).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void testRequiresNewPropagation() {
        // This test verifies that service methods can be called with different transaction propagation
        long initialCount = gratitudeEntryRepository.count();

        // Save in new transaction
        GratitudeEntryDTO savedEntry = gratitudeEntryService.save(gratitudeEntryDTO);

        assertThat(savedEntry.getId()).isNotNull();
        assertThat(gratitudeEntryRepository.count()).isEqualTo(initialCount + 1);

        // Verify transaction boundary
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
    }

    @Test
    void testConcurrentTransactionIsolation() throws InterruptedException {
        // Create base entry
        GratitudeEntryDTO savedEntry = transactionTemplate.execute(status -> {
            return gratitudeEntryService.save(gratitudeEntryDTO);
        });

        final String[] results = new String[2];
        final Exception[] exceptions = new Exception[2];

        // Thread 1: Update entry
        Thread thread1 = new Thread(() -> {
            try {
                // Setup security context for thread
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
                SecurityContextHolder.setContext(securityContext);

                transactionTemplate.execute(status -> {
                    try {
                        Thread.sleep(100); // Small delay to increase chance of concurrent access
                        savedEntry.setEntry("Updated by thread 1");
                        GratitudeEntryDTO updated = gratitudeEntryService.update(savedEntry);
                        results[0] = updated.getEntry();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                    return null;
                });
            } catch (Exception e) {
                exceptions[0] = e;
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        // Thread 2: Also try to update the same entry
        Thread thread2 = new Thread(() -> {
            try {
                // Setup security context for thread
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
                SecurityContextHolder.setContext(securityContext);

                transactionTemplate.execute(status -> {
                    try {
                        Thread.sleep(50); // Different delay
                        savedEntry.setEntry("Updated by thread 2");
                        GratitudeEntryDTO updated = gratitudeEntryService.update(savedEntry);
                        results[1] = updated.getEntry();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                    return null;
                });
            } catch (Exception e) {
                exceptions[1] = e;
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        thread1.start();
        thread2.start();

        thread1.join(5000); // 5 second timeout
        thread2.join(5000);

        // At least one update should succeed
        // The exact behavior depends on database isolation level and locking strategy
        boolean atLeastOneSucceeded = (exceptions[0] == null) || (exceptions[1] == null);
        assertThat(atLeastOneSucceeded).isTrue();

        // Verify final state is consistent
        Optional<GratitudeEntry> finalEntry = gratitudeEntryRepository.findById(savedEntry.getId());
        assertThat(finalEntry).isPresent();
        assertThat(finalEntry.orElseThrow().getEntry()).containsAnyOf("Updated by thread 1", "Updated by thread 2");
    }

    @Test
    void testTransactionTimeoutHandling() {
        // This test verifies that long-running transactions are handled properly
        // Note: In a real scenario, you might configure transaction timeout

        long initialCount = gratitudeEntryRepository.count();

        transactionTemplate.execute(status -> {
            // Simulate some processing time
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            GratitudeEntryDTO savedEntry = gratitudeEntryService.save(gratitudeEntryDTO);
            assertThat(savedEntry.getId()).isNotNull();

            return savedEntry;
        });

        assertThat(gratitudeEntryRepository.count()).isEqualTo(initialCount + 1);
    }

    @BeforeTransaction
    void beforeTransaction() {
        // Setup that needs to happen before each transaction
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    }

    @AfterTransaction
    void afterTransaction() {
        // Cleanup after each transaction
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    }
}
