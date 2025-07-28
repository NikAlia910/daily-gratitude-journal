package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.domain.GratitudeEntry;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.Mood;
import com.mycompany.myapp.repository.GratitudeEntryRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.dto.GratitudeEntryDTO;
import com.mycompany.myapp.service.mapper.GratitudeEntryMapper;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class GratitudeEntryServiceTest {

    private static final String DEFAULT_LOGIN = "testuser";
    private static final String DEFAULT_ENTRY_TEXT = "I am grateful for my health";
    private static final LocalDate DEFAULT_DATE = LocalDate.of(2024, 1, 1);
    private static final Mood DEFAULT_MOOD = Mood.GRATEFUL;
    private static final Long DEFAULT_ID = 1L;

    @Mock
    private GratitudeEntryRepository gratitudeEntryRepository;

    @Mock
    private GratitudeEntryMapper gratitudeEntryMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GratitudeEntryService gratitudeEntryService;

    private User testUser;
    private GratitudeEntry gratitudeEntry;
    private GratitudeEntryDTO gratitudeEntryDTO;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        gratitudeEntry = createGratitudeEntry();
        gratitudeEntryDTO = createGratitudeEntryDTO();
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setLogin(DEFAULT_LOGIN);
        user.setEmail("test@example.com");
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setActivated(true);
        return user;
    }

    private GratitudeEntry createGratitudeEntry() {
        GratitudeEntry entry = new GratitudeEntry();
        entry.setId(DEFAULT_ID);
        entry.setDate(DEFAULT_DATE);
        entry.setEntry(DEFAULT_ENTRY_TEXT);
        entry.setMood(DEFAULT_MOOD);
        entry.setTimestamp(Instant.now());
        entry.setUser(testUser);
        return entry;
    }

    private GratitudeEntryDTO createGratitudeEntryDTO() {
        GratitudeEntryDTO dto = new GratitudeEntryDTO();
        dto.setId(DEFAULT_ID);
        dto.setDate(DEFAULT_DATE);
        dto.setEntry(DEFAULT_ENTRY_TEXT);
        dto.setMood(DEFAULT_MOOD);
        dto.setTimestamp(Instant.now());
        return dto;
    }

    @Test
    void save_WhenValidEntry_ShouldSaveSuccessfully() {
        // Given
        GratitudeEntryDTO inputDTO = new GratitudeEntryDTO();
        inputDTO.setDate(DEFAULT_DATE);
        inputDTO.setEntry(DEFAULT_ENTRY_TEXT);
        inputDTO.setMood(DEFAULT_MOOD);

        when(gratitudeEntryRepository.existsByUserIsCurrentUserAndDate(DEFAULT_DATE)).thenReturn(false);
        when(gratitudeEntryMapper.toEntity(any(GratitudeEntryDTO.class))).thenReturn(gratitudeEntry);
        when(gratitudeEntryRepository.save(any(GratitudeEntry.class))).thenReturn(gratitudeEntry);
        when(gratitudeEntryMapper.toDto(any(GratitudeEntry.class))).thenReturn(gratitudeEntryDTO);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(testUser));

            // When
            GratitudeEntryDTO result = gratitudeEntryService.save(inputDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEntry()).isEqualTo(DEFAULT_ENTRY_TEXT);
            verify(gratitudeEntryRepository).save(any(GratitudeEntry.class));
        }
    }

    @Test
    void save_WhenEmptyEntry_ShouldThrowException() {
        // Given
        GratitudeEntryDTO inputDTO = new GratitudeEntryDTO();
        inputDTO.setDate(DEFAULT_DATE);
        inputDTO.setEntry(""); // Empty entry
        inputDTO.setMood(DEFAULT_MOOD);

        // When & Then
        assertThatThrownBy(() -> gratitudeEntryService.save(inputDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Entry text cannot be empty");

        verify(gratitudeEntryRepository, never()).save(any(GratitudeEntry.class));
    }

    @Test
    void save_WhenNullEntry_ShouldThrowException() {
        // Given
        GratitudeEntryDTO inputDTO = new GratitudeEntryDTO();
        inputDTO.setDate(DEFAULT_DATE);
        inputDTO.setEntry(null); // Null entry
        inputDTO.setMood(DEFAULT_MOOD);

        // When & Then
        assertThatThrownBy(() -> gratitudeEntryService.save(inputDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Entry text cannot be empty");

        verify(gratitudeEntryRepository, never()).save(any(GratitudeEntry.class));
    }

    @Test
    void save_WhenDuplicateDateForUser_ShouldThrowException() {
        // Given
        GratitudeEntryDTO inputDTO = new GratitudeEntryDTO();
        inputDTO.setDate(DEFAULT_DATE);
        inputDTO.setEntry(DEFAULT_ENTRY_TEXT);
        inputDTO.setMood(DEFAULT_MOOD);

        when(gratitudeEntryRepository.existsByUserIsCurrentUserAndDate(DEFAULT_DATE)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> gratitudeEntryService.save(inputDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("An entry already exists for this date");

        verify(gratitudeEntryRepository, never()).save(any(GratitudeEntry.class));
    }

    @Test
    void save_ShouldSetCurrentUserAndTimestamp() {
        // Given
        GratitudeEntryDTO inputDTO = new GratitudeEntryDTO();
        inputDTO.setDate(DEFAULT_DATE);
        inputDTO.setEntry(DEFAULT_ENTRY_TEXT);
        inputDTO.setMood(DEFAULT_MOOD);

        GratitudeEntry capturableEntry = new GratitudeEntry();
        when(gratitudeEntryRepository.existsByUserIsCurrentUserAndDate(DEFAULT_DATE)).thenReturn(false);
        when(gratitudeEntryMapper.toEntity(any(GratitudeEntryDTO.class))).thenReturn(capturableEntry);
        when(gratitudeEntryRepository.save(any(GratitudeEntry.class))).thenReturn(gratitudeEntry);
        when(gratitudeEntryMapper.toDto(any(GratitudeEntry.class))).thenReturn(gratitudeEntryDTO);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(testUser));

            // When
            gratitudeEntryService.save(inputDTO);

            // Then
            verify(gratitudeEntryRepository).save(
                argThat(
                    entry -> entry.getUser() != null && entry.getUser().getLogin().equals(DEFAULT_LOGIN) && entry.getTimestamp() != null
                )
            );
        }
    }

    @Test
    void update_WhenValidEntry_ShouldUpdateSuccessfully() {
        // Given
        GratitudeEntryDTO inputDTO = new GratitudeEntryDTO();
        inputDTO.setId(DEFAULT_ID);
        inputDTO.setDate(DEFAULT_DATE);
        inputDTO.setEntry("Updated gratitude text");
        inputDTO.setMood(Mood.HAPPY);

        GratitudeEntry existingEntry = createGratitudeEntry();
        Instant originalTimestamp = existingEntry.getTimestamp();

        when(gratitudeEntryRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(existingEntry));
        when(gratitudeEntryMapper.toEntity(any(GratitudeEntryDTO.class))).thenReturn(gratitudeEntry);
        when(gratitudeEntryRepository.save(any(GratitudeEntry.class))).thenReturn(gratitudeEntry);
        when(gratitudeEntryMapper.toDto(any(GratitudeEntry.class))).thenReturn(gratitudeEntryDTO);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));

            // When
            GratitudeEntryDTO result = gratitudeEntryService.update(inputDTO);

            // Then
            assertThat(result).isNotNull();
            verify(gratitudeEntryRepository).save(
                argThat(
                    entry ->
                        entry.getTimestamp().equals(originalTimestamp) && // Original timestamp preserved
                        entry.getUser().equals(testUser) // Original user preserved
                )
            );
        }
    }

    @Test
    void update_WhenEntryNotFound_ShouldThrowException() {
        // Given
        GratitudeEntryDTO inputDTO = new GratitudeEntryDTO();
        inputDTO.setId(DEFAULT_ID);
        inputDTO.setEntry(DEFAULT_ENTRY_TEXT);

        when(gratitudeEntryRepository.findById(DEFAULT_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> gratitudeEntryService.update(inputDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Entity not found");
    }

    @Test
    void findAll_ShouldReturnUserEntriesOnly() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<GratitudeEntry> entries = Arrays.asList(gratitudeEntry);
        Page<GratitudeEntry> entryPage = new PageImpl<>(entries, pageable, entries.size());

        when(gratitudeEntryRepository.findByUserIsCurrentUser(pageable)).thenReturn(entryPage);
        when(gratitudeEntryMapper.toDto(any(GratitudeEntry.class))).thenReturn(gratitudeEntryDTO);

        // When
        Page<GratitudeEntryDTO> result = gratitudeEntryService.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(gratitudeEntryRepository).findByUserIsCurrentUser(pageable);
    }

    @Test
    void findByDate_WhenEntryExists_ShouldReturnEntry() {
        // Given
        when(gratitudeEntryRepository.findByUserIsCurrentUserAndDate(DEFAULT_DATE)).thenReturn(Optional.of(gratitudeEntry));
        when(gratitudeEntryMapper.toDto(gratitudeEntry)).thenReturn(gratitudeEntryDTO);

        // When
        Optional<GratitudeEntryDTO> result = gratitudeEntryService.findByDate(DEFAULT_DATE);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getDate()).isEqualTo(DEFAULT_DATE);
        verify(gratitudeEntryRepository).findByUserIsCurrentUserAndDate(DEFAULT_DATE);
    }

    @Test
    void findByDate_WhenEntryNotExists_ShouldReturnEmpty() {
        // Given
        when(gratitudeEntryRepository.findByUserIsCurrentUserAndDate(DEFAULT_DATE)).thenReturn(Optional.empty());

        // When
        Optional<GratitudeEntryDTO> result = gratitudeEntryService.findByDate(DEFAULT_DATE);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByDateRange_ShouldReturnEntriesInRange() {
        // Given
        LocalDate startDate = DEFAULT_DATE;
        LocalDate endDate = DEFAULT_DATE.plusDays(7);
        Pageable pageable = PageRequest.of(0, 10);
        List<GratitudeEntry> entries = Arrays.asList(gratitudeEntry);
        Page<GratitudeEntry> entryPage = new PageImpl<>(entries, pageable, entries.size());

        when(gratitudeEntryRepository.findByUserIsCurrentUserAndDateBetween(startDate, endDate, pageable)).thenReturn(entryPage);
        when(gratitudeEntryMapper.toDto(any(GratitudeEntry.class))).thenReturn(gratitudeEntryDTO);

        // When
        Page<GratitudeEntryDTO> result = gratitudeEntryService.findByDateRange(startDate, endDate, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(gratitudeEntryRepository).findByUserIsCurrentUserAndDateBetween(startDate, endDate, pageable);
    }

    @Test
    void findOne_WhenEntryBelongsToCurrentUser_ShouldReturnEntry() {
        // Given
        when(gratitudeEntryRepository.findOneWithEagerRelationships(DEFAULT_ID)).thenReturn(Optional.of(gratitudeEntry));
        when(gratitudeEntryMapper.toDto(gratitudeEntry)).thenReturn(gratitudeEntryDTO);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));

            // When
            Optional<GratitudeEntryDTO> result = gratitudeEntryService.findOne(DEFAULT_ID);

            // Then
            assertThat(result).isPresent();
            verify(gratitudeEntryRepository).findOneWithEagerRelationships(DEFAULT_ID);
        }
    }

    @Test
    void findOne_WhenEntryBelongsToOtherUser_ShouldReturnEmpty() {
        // Given
        User otherUser = createTestUser();
        otherUser.setLogin("otheruser");
        gratitudeEntry.setUser(otherUser);

        when(gratitudeEntryRepository.findOneWithEagerRelationships(DEFAULT_ID)).thenReturn(Optional.of(gratitudeEntry));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));

            // When
            Optional<GratitudeEntryDTO> result = gratitudeEntryService.findOne(DEFAULT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Test
    void delete_WhenEntryBelongsToCurrentUser_ShouldDelete() {
        // Given
        when(gratitudeEntryRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(gratitudeEntry));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));

            // When
            gratitudeEntryService.delete(DEFAULT_ID);

            // Then
            verify(gratitudeEntryRepository).deleteById(DEFAULT_ID);
        }
    }

    @Test
    void delete_WhenEntryBelongsToOtherUser_ShouldThrowException() {
        // Given
        User otherUser = createTestUser();
        otherUser.setLogin("otheruser");
        gratitudeEntry.setUser(otherUser);

        when(gratitudeEntryRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(gratitudeEntry));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));

            // When & Then
            assertThatThrownBy(() -> gratitudeEntryService.delete(DEFAULT_ID))
                .isInstanceOf(BadRequestAlertException.class)
                .hasMessageContaining("does not belong to current user");

            verify(gratitudeEntryRepository, never()).deleteById(DEFAULT_ID);
        }
    }

    @Test
    void delete_WhenEntryNotFound_ShouldThrowException() {
        // Given
        when(gratitudeEntryRepository.findById(DEFAULT_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> gratitudeEntryService.delete(DEFAULT_ID))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Entry not found");

        verify(gratitudeEntryRepository, never()).deleteById(DEFAULT_ID);
    }

    @Test
    void findAllForCurrentUser_ShouldReturnUserEntriesAsList() {
        // Given
        List<GratitudeEntry> entries = Arrays.asList(gratitudeEntry);
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "date", "timestamp"));
        when(gratitudeEntryRepository.findByUserIsCurrentUser(any(Pageable.class))).thenReturn(
            new PageImpl<>(entries, pageable, entries.size())
        );
        when(gratitudeEntryMapper.toDto(gratitudeEntry)).thenReturn(gratitudeEntryDTO);

        // When
        List<GratitudeEntryDTO> result = gratitudeEntryService.findAllForCurrentUser();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntry()).isEqualTo(DEFAULT_ENTRY_TEXT);
        verify(gratitudeEntryRepository).findByUserIsCurrentUser(any(Pageable.class));
    }
}
