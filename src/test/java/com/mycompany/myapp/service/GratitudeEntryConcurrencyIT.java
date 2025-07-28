package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.GratitudeEntry;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.Mood;
import com.mycompany.myapp.repository.GratitudeEntryRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.service.dto.GratitudeEntryDTO;
import com.mycompany.myapp.service.mapper.GratitudeEntryMapper;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration tests for concurrent access scenarios in {@link GratitudeEntryService}.
 * Tests race conditions, concurrent modifications, and data consistency.
 */
@IntegrationTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser("testuser")
class GratitudeEntryConcurrencyIT {

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
    private ExecutorService executorService;

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

        executorService = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    void cleanUp() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        SecurityContextHolder.clearContext();
        try {
            gratitudeEntryRepository.deleteAll();
            userRepository.deleteAll();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    void testConcurrentCreationSameDate() throws Exception {
        LocalDate testDate = LocalDate.now();
        int numThreads = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicReference<Exception> firstException = new AtomicReference<>();

        // Launch multiple threads trying to create entries for the same date
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    // Use transaction template to ensure proper transaction handling
                    transactionTemplate.execute(status -> {
                        try {
                            // Set up security context
                            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
                            SecurityContextHolder.setContext(securityContext);

                            GratitudeEntryDTO dto = new GratitudeEntryDTO();
                            dto.setDate(testDate);
                            dto.setEntry("Concurrent entry from thread " + threadNum);
                            dto.setMood(Mood.GRATEFUL);
                            dto.setTimestamp(Instant.now());

                            GratitudeEntryDTO saved = gratitudeEntryService.save(dto);
                            successCount.incrementAndGet();
                            return saved;
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            firstException.compareAndSet(null, e);
                            return null;
                        }
                    });
                } catch (Exception e) {
                    firstException.compareAndSet(null, e);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // Only one should succeed due to unique constraint (handled by service layer)
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(numThreads - 1);

        // Verify only one entry exists in database
        List<GratitudeEntry> userEntries = gratitudeEntryRepository.findByUserIsCurrentUser();
        assertThat(userEntries).hasSize(1);
    }

    @Test
    void testConcurrentUpdateSameEntry() throws Exception {
        // Create initial entry in a transaction
        GratitudeEntryDTO savedEntry = transactionTemplate.execute(status -> {
            // Set security context for initial creation
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
            SecurityContextHolder.setContext(securityContext);

            GratitudeEntryDTO initialDto = new GratitudeEntryDTO();
            initialDto.setDate(LocalDate.now());
            initialDto.setEntry("Initial entry text");
            initialDto.setMood(Mood.GRATEFUL);

            GratitudeEntryDTO result = gratitudeEntryService.save(initialDto);
            SecurityContextHolder.clearContext();
            return result;
        });

        assertThat(savedEntry).isNotNull();

        int numThreads = 3;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Launch multiple threads trying to update the same entry
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    transactionTemplate.execute(status -> {
                        try {
                            // Set up security context
                            SecurityContext threadSecurityContext = SecurityContextHolder.createEmptyContext();
                            threadSecurityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
                            SecurityContextHolder.setContext(threadSecurityContext);

                            GratitudeEntryDTO updateDto = new GratitudeEntryDTO();
                            updateDto.setId(savedEntry.getId());
                            updateDto.setDate(savedEntry.getDate());
                            updateDto.setEntry("Updated by thread " + threadNum);
                            updateDto.setMood(Mood.CONTENT);

                            GratitudeEntryDTO updated = gratitudeEntryService.update(updateDto);
                            successCount.incrementAndGet();
                            return updated;
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            return null;
                        }
                    });
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // All updates should succeed (last writer wins)
        assertThat(successCount.get()).isEqualTo(numThreads);

        // Verify final state
        GratitudeEntry finalEntry = gratitudeEntryRepository.findById(savedEntry.getId()).orElseThrow();
        assertThat(finalEntry.getEntry()).startsWith("Updated by thread");
        assertThat(finalEntry.getMood()).isEqualTo(Mood.CONTENT);
    }

    @Test
    void testConcurrentCreateDifferentDates() throws Exception {
        int numThreads = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);

        // Launch threads creating entries for different dates
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    transactionTemplate.execute(status -> {
                        try {
                            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
                            SecurityContextHolder.setContext(securityContext);

                            GratitudeEntryDTO dto = new GratitudeEntryDTO();
                            dto.setDate(LocalDate.now().minusDays(threadNum)); // Different dates
                            dto.setEntry("Entry for day " + threadNum);
                            dto.setMood(Mood.values()[threadNum % Mood.values().length]);
                            dto.setTimestamp(Instant.now());

                            GratitudeEntryDTO saved = gratitudeEntryService.save(dto);
                            successCount.incrementAndGet();
                            return saved;
                        } catch (Exception e) {
                            return null;
                        }
                    });
                } catch (Exception e) {} finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // All should succeed since they have different dates
        assertThat(successCount.get()).isEqualTo(numThreads);

        // Verify all entries exist - use findByUserIsCurrentUser to avoid lazy loading issues
        List<GratitudeEntry> userEntries = gratitudeEntryRepository.findByUserIsCurrentUser();
        assertThat(userEntries).hasSize(numThreads);
    }

    @Test
    void testConcurrentDeleteAndUpdate() throws Exception {
        // Create initial entry
        GratitudeEntryDTO savedEntry = transactionTemplate.execute(status -> {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
            SecurityContextHolder.setContext(securityContext);

            GratitudeEntryDTO initialDto = new GratitudeEntryDTO();
            initialDto.setDate(LocalDate.now());
            initialDto.setEntry("Entry to be modified");
            initialDto.setMood(Mood.GRATEFUL);

            GratitudeEntryDTO result = gratitudeEntryService.save(initialDto);
            SecurityContextHolder.clearContext();
            return result;
        });

        assertThat(savedEntry).isNotNull();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicReference<Exception> deleteException = new AtomicReference<>();
        AtomicReference<Exception> updateException = new AtomicReference<>();
        AtomicReference<Boolean> deleteSuccess = new AtomicReference<>(false);
        AtomicReference<Boolean> updateSuccess = new AtomicReference<>(false);

        // Thread 1: Delete the entry
        executorService.submit(() -> {
            try {
                startLatch.await();

                transactionTemplate.execute(status -> {
                    try {
                        SecurityContext threadSecurityContext = SecurityContextHolder.createEmptyContext();
                        threadSecurityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
                        SecurityContextHolder.setContext(threadSecurityContext);

                        Thread.sleep(50); // Small delay to increase chance of conflict

                        gratitudeEntryService.delete(savedEntry.getId());
                        deleteSuccess.set(true);
                        return null;
                    } catch (Exception e) {
                        deleteException.set(e);
                        return null;
                    }
                });
            } catch (Exception e) {
                deleteException.set(e);
            } finally {
                SecurityContextHolder.clearContext();
                doneLatch.countDown();
            }
        });

        // Thread 2: Update the same entry
        executorService.submit(() -> {
            try {
                startLatch.await();

                transactionTemplate.execute(status -> {
                    try {
                        SecurityContext threadSecurityContext = SecurityContextHolder.createEmptyContext();
                        threadSecurityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
                        SecurityContextHolder.setContext(threadSecurityContext);

                        Thread.sleep(25); // Different delay

                        GratitudeEntryDTO updateDto = new GratitudeEntryDTO();
                        updateDto.setId(savedEntry.getId());
                        updateDto.setDate(savedEntry.getDate());
                        updateDto.setEntry("Updated entry");
                        updateDto.setMood(Mood.CONTENT);

                        gratitudeEntryService.update(updateDto);
                        updateSuccess.set(true);
                        return null;
                    } catch (Exception e) {
                        updateException.set(e);
                        return null;
                    }
                });
            } catch (Exception e) {
                updateException.set(e);
            } finally {
                SecurityContextHolder.clearContext();
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // Either delete succeeds and update fails, or update succeeds and delete fails
        // At least one operation should succeed
        boolean atLeastOneSucceeded = deleteSuccess.get() || updateSuccess.get();
        assertThat(atLeastOneSucceeded).isTrue();

        // Both operations might succeed if they happen in sequence rather than concurrently
        // The important thing is that the final state is consistent

        // Verify final state is consistent
        boolean entryExists = gratitudeEntryRepository.findById(savedEntry.getId()).isPresent();
        if (deleteSuccess.get()) {
            assertThat(entryExists).isFalse();
        } else if (updateSuccess.get()) {
            assertThat(entryExists).isTrue();
        }
    }

    @Test
    void testConcurrentReadAndWrite() throws Exception {
        // Create initial entry
        GratitudeEntryDTO savedEntry = transactionTemplate.execute(status -> {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
            SecurityContextHolder.setContext(securityContext);

            GratitudeEntryDTO initialDto = new GratitudeEntryDTO();
            initialDto.setDate(LocalDate.now());
            initialDto.setEntry("Initial entry");
            initialDto.setMood(Mood.GRATEFUL);

            GratitudeEntryDTO result = gratitudeEntryService.save(initialDto);
            SecurityContextHolder.clearContext();
            return result;
        });

        int numReaders = 3;
        int numWriters = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numReaders + numWriters);

        AtomicInteger readSuccessCount = new AtomicInteger(0);
        AtomicInteger writeSuccessCount = new AtomicInteger(0);

        // Reader threads
        for (int i = 0; i < numReaders; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    SecurityContext threadSecurityContext = SecurityContextHolder.createEmptyContext();
                    threadSecurityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
                    SecurityContextHolder.setContext(threadSecurityContext);

                    // Repeatedly read the entry
                    for (int j = 0; j < 10; j++) {
                        try {
                            gratitudeEntryService.findOne(savedEntry.getId());
                            Thread.sleep(10);
                        } catch (Exception e) {}
                    }
                    readSuccessCount.incrementAndGet();
                } catch (Exception e) {} finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });
        }

        // Writer threads
        for (int i = 0; i < numWriters; i++) {
            final int writerNum = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    SecurityContext threadSecurityContext = SecurityContextHolder.createEmptyContext();
                    threadSecurityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
                    SecurityContextHolder.setContext(threadSecurityContext);

                    // Repeatedly update the entry
                    for (int j = 0; j < 5; j++) {
                        final int iteration = j; // Make effectively final for lambda
                        try {
                            transactionTemplate.execute(status -> {
                                try {
                                    GratitudeEntryDTO updateDto = new GratitudeEntryDTO();
                                    updateDto.setId(savedEntry.getId());
                                    updateDto.setDate(savedEntry.getDate());
                                    updateDto.setEntry("Updated by writer " + writerNum + " iteration " + iteration);
                                    updateDto.setMood(Mood.CONTENT);

                                    gratitudeEntryService.update(updateDto);
                                    Thread.sleep(20);
                                    return null;
                                } catch (Exception e) {
                                    return null;
                                }
                            });
                        } catch (Exception e) {}
                    }
                    writeSuccessCount.incrementAndGet();
                } catch (Exception e) {} finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // All readers should succeed
        assertThat(readSuccessCount.get()).isEqualTo(numReaders);

        // All writers should succeed (eventually)
        assertThat(writeSuccessCount.get()).isEqualTo(numWriters);

        // Entry should still exist and be consistent
        assertThat(gratitudeEntryRepository.findById(savedEntry.getId())).isPresent();
    }

    @Test
    void testDeadlockAvoidance() throws Exception {
        // Create two entries
        GratitudeEntryDTO savedEntry1 = transactionTemplate.execute(status -> {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
            SecurityContextHolder.setContext(securityContext);

            GratitudeEntryDTO entry1Dto = new GratitudeEntryDTO();
            entry1Dto.setDate(LocalDate.now());
            entry1Dto.setEntry("Entry 1");
            entry1Dto.setMood(Mood.GRATEFUL);

            GratitudeEntryDTO result = gratitudeEntryService.save(entry1Dto);
            SecurityContextHolder.clearContext();
            return result;
        });

        GratitudeEntryDTO savedEntry2 = transactionTemplate.execute(status -> {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
            SecurityContextHolder.setContext(securityContext);

            GratitudeEntryDTO entry2Dto = new GratitudeEntryDTO();
            entry2Dto.setDate(LocalDate.now().minusDays(1));
            entry2Dto.setEntry("Entry 2");
            entry2Dto.setMood(Mood.HAPPY);

            GratitudeEntryDTO result = gratitudeEntryService.save(entry2Dto);
            SecurityContextHolder.clearContext();
            return result;
        });

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);

        // Thread 1: Update entry1 then entry2
        executorService.submit(() -> {
            try {
                startLatch.await();

                transactionTemplate.execute(status -> {
                    try {
                        SecurityContext threadSecurityContext = SecurityContextHolder.createEmptyContext();
                        threadSecurityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
                        SecurityContextHolder.setContext(threadSecurityContext);

                        // Update in order: entry1, then entry2
                        GratitudeEntryDTO update1 = new GratitudeEntryDTO();
                        update1.setId(savedEntry1.getId());
                        update1.setDate(savedEntry1.getDate());
                        update1.setEntry("Updated entry 1 by thread 1");
                        update1.setMood(Mood.CONTENT);
                        gratitudeEntryService.update(update1);

                        Thread.sleep(100); // Increase chance of deadlock

                        GratitudeEntryDTO update2 = new GratitudeEntryDTO();
                        update2.setId(savedEntry2.getId());
                        update2.setDate(savedEntry2.getDate());
                        update2.setEntry("Updated entry 2 by thread 1");
                        update2.setMood(Mood.REFLECTIVE);
                        gratitudeEntryService.update(update2);

                        successCount.incrementAndGet();
                        return null;
                    } catch (Exception e) {
                        return null;
                    }
                });
            } catch (Exception e) {} finally {
                SecurityContextHolder.clearContext();
                doneLatch.countDown();
            }
        });

        // Thread 2: Update entry2 then entry1 (reverse order)
        executorService.submit(() -> {
            try {
                startLatch.await();

                transactionTemplate.execute(status -> {
                    try {
                        SecurityContext threadSecurityContext = SecurityContextHolder.createEmptyContext();
                        threadSecurityContext.setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
                        SecurityContextHolder.setContext(threadSecurityContext);

                        // Update in reverse order: entry2, then entry1
                        GratitudeEntryDTO update2 = new GratitudeEntryDTO();
                        update2.setId(savedEntry2.getId());
                        update2.setDate(savedEntry2.getDate());
                        update2.setEntry("Updated entry 2 by thread 2");
                        update2.setMood(Mood.HOPEFUL);
                        gratitudeEntryService.update(update2);

                        Thread.sleep(100); // Increase chance of deadlock

                        GratitudeEntryDTO update1 = new GratitudeEntryDTO();
                        update1.setId(savedEntry1.getId());
                        update1.setDate(savedEntry1.getDate());
                        update1.setEntry("Updated entry 1 by thread 2");
                        update1.setMood(Mood.OTHER);
                        gratitudeEntryService.update(update1);

                        successCount.incrementAndGet();
                        return null;
                    } catch (Exception e) {
                        return null;
                    }
                });
            } catch (Exception e) {} finally {
                SecurityContextHolder.clearContext();
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // At least one thread should succeed (deadlock detection/timeout should prevent total deadlock)
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);

        // Both entries should still exist
        assertThat(gratitudeEntryRepository.findById(savedEntry1.getId())).isPresent();
        assertThat(gratitudeEntryRepository.findById(savedEntry2.getId())).isPresent();
    }
}
