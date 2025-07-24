package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.config.Constants;
import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.dto.UserDTO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.jhipster.security.RandomUtil;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String DEFAULT_LOGIN = "testuser";
    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final String DEFAULT_FIRSTNAME = "John";
    private static final String DEFAULT_LASTNAME = "Doe";
    private static final String DEFAULT_PASSWORD = "password";
    private static final String DEFAULT_LANGKEY = "en";
    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";
    private static final Long DEFAULT_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private AdminUserDTO testUserDTO;
    private Authority userAuthority;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testUserDTO = createTestUserDTO();
        userAuthority = createUserAuthority();
    }

    private User createTestUser() {
        User user = new User();
        user.setId(DEFAULT_ID);
        user.setLogin(DEFAULT_LOGIN);
        user.setEmail(DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setActivated(true);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setImageUrl(DEFAULT_IMAGEURL);
        return user;
    }

    private AdminUserDTO createTestUserDTO() {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(DEFAULT_ID);
        dto.setLogin(DEFAULT_LOGIN);
        dto.setEmail(DEFAULT_EMAIL);
        dto.setFirstName(DEFAULT_FIRSTNAME);
        dto.setLastName(DEFAULT_LASTNAME);
        dto.setActivated(true);
        dto.setLangKey(DEFAULT_LANGKEY);
        dto.setImageUrl(DEFAULT_IMAGEURL);
        Set<String> authorities = new HashSet<>();
        authorities.add(AuthoritiesConstants.USER);
        dto.setAuthorities(authorities);
        return dto;
    }

    private Authority createUserAuthority() {
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        return authority;
    }

    @Test
    void activateRegistration_WithValidKey_ShouldActivateUser() {
        // Given
        String activationKey = "activation-key";
        User inactiveUser = createTestUser();
        inactiveUser.setActivated(false);
        inactiveUser.setActivationKey(activationKey);

        when(userRepository.findOneByActivationKey(activationKey)).thenReturn(Optional.of(inactiveUser));
        when(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(cache);
        when(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(cache);

        // When
        Optional<User> result = userService.activateRegistration(activationKey);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().isActivated()).isTrue();
        assertThat(result.orElseThrow().getActivationKey()).isNull();
        verify(cache).evictIfPresent(inactiveUser.getLogin());
        verify(cache).evictIfPresent(inactiveUser.getEmail());
    }

    @Test
    void activateRegistration_WithInvalidKey_ShouldReturnEmpty() {
        // Given
        String invalidKey = "invalid-key";
        when(userRepository.findOneByActivationKey(invalidKey)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.activateRegistration(invalidKey);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void completePasswordReset_WithValidKeyAndNotExpired_ShouldResetPassword() {
        // Given
        String newPassword = "newpassword";
        String resetKey = "reset-key";
        String encodedPassword = "encoded-password";

        User user = createTestUser();
        user.setResetKey(resetKey);
        user.setResetDate(Instant.now().minus(1, ChronoUnit.HOURS)); // 1 hour ago, not expired

        when(userRepository.findOneByResetKey(resetKey)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(cache);
        when(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(cache);

        // When
        Optional<User> result = userService.completePasswordReset(newPassword, resetKey);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getPassword()).isEqualTo(encodedPassword);
        assertThat(result.orElseThrow().getResetKey()).isNull();
        assertThat(result.orElseThrow().getResetDate()).isNull();
        verify(cache).evictIfPresent(user.getLogin());
        verify(cache).evictIfPresent(user.getEmail());
    }

    @Test
    void completePasswordReset_WithExpiredKey_ShouldReturnEmpty() {
        // Given
        String newPassword = "newpassword";
        String resetKey = "expired-key";

        User user = createTestUser();
        user.setResetKey(resetKey);
        user.setResetDate(Instant.now().minus(25, ChronoUnit.HOURS)); // 25 hours ago, expired

        when(userRepository.findOneByResetKey(resetKey)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.completePasswordReset(newPassword, resetKey);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void completePasswordReset_WithInvalidKey_ShouldReturnEmpty() {
        // Given
        String newPassword = "newpassword";
        String invalidKey = "invalid-key";

        when(userRepository.findOneByResetKey(invalidKey)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.completePasswordReset(newPassword, invalidKey);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void requestPasswordReset_WithValidEmailAndActivatedUser_ShouldGenerateResetKey() {
        // Given
        String email = DEFAULT_EMAIL;
        User user = createTestUser();
        user.setActivated(true);

        when(userRepository.findOneByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(cache);
        when(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(cache);

        // When
        Optional<User> result = userService.requestPasswordReset(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getResetKey()).isNotNull();
        assertThat(result.orElseThrow().getResetDate()).isNotNull();
        verify(cache).evictIfPresent(user.getLogin());
        verify(cache).evictIfPresent(user.getEmail());
    }

    @Test
    void requestPasswordReset_WithInactiveUser_ShouldReturnEmpty() {
        // Given
        String email = DEFAULT_EMAIL;
        User user = createTestUser();
        user.setActivated(false);

        when(userRepository.findOneByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.requestPasswordReset(email);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void requestPasswordReset_WithInvalidEmail_ShouldReturnEmpty() {
        // Given
        String invalidEmail = "invalid@example.com";

        when(userRepository.findOneByEmailIgnoreCase(invalidEmail)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.requestPasswordReset(invalidEmail);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void registerUser_WithValidData_ShouldCreateNewUser() {
        // Given
        AdminUserDTO userDTO = createTestUserDTO();
        userDTO.setId(null); // New user shouldn't have ID
        String password = DEFAULT_PASSWORD;
        String encodedPassword = "encoded-password";

        when(userRepository.findOneByLogin(DEFAULT_LOGIN.toLowerCase())).thenReturn(Optional.empty());
        when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(userAuthority));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(cache);
        when(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(cache);

        // When
        User result = userService.registerUser(userDTO, password);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLogin()).isEqualTo(DEFAULT_LOGIN.toLowerCase());
        assertThat(result.getPassword()).isEqualTo(encodedPassword);
        assertThat(result.isActivated()).isFalse();
        assertThat(result.getActivationKey()).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(cache, times(2)).evictIfPresent(any()); // Called twice: login cache and email cache
    }

    @Test
    void registerUser_WithExistingLogin_ShouldThrowException() {
        // Given
        AdminUserDTO userDTO = createTestUserDTO();
        userDTO.setId(null);
        String password = DEFAULT_PASSWORD;

        User existingUser = createTestUser();
        existingUser.setActivated(true); // Activated user cannot be removed

        when(userRepository.findOneByLogin(DEFAULT_LOGIN.toLowerCase())).thenReturn(Optional.of(existingUser));

        // When/Then
        assertThatThrownBy(() -> userService.registerUser(userDTO, password)).isInstanceOf(UsernameAlreadyUsedException.class);
    }

    @Test
    void registerUser_WithExistingEmail_ShouldThrowException() {
        // Given
        AdminUserDTO userDTO = createTestUserDTO();
        userDTO.setId(null);
        String password = DEFAULT_PASSWORD;

        User existingUser = createTestUser();
        existingUser.setActivated(true); // Activated user cannot be removed

        when(userRepository.findOneByLogin(DEFAULT_LOGIN.toLowerCase())).thenReturn(Optional.empty());
        when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.of(existingUser));

        // When/Then
        assertThatThrownBy(() -> userService.registerUser(userDTO, password)).isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void createUser_WithValidData_ShouldCreateActivatedUser() {
        // Given
        AdminUserDTO userDTO = createTestUserDTO();
        userDTO.setId(null);
        String randomPassword = "random-password";
        String encodedPassword = "encoded-password";

        when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(userAuthority));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(cache);
        when(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(cache);

        // When
        User result = userService.createUser(userDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLogin()).isEqualTo(DEFAULT_LOGIN.toLowerCase());
        assertThat(result.isActivated()).isTrue();
        assertThat(result.getResetKey()).isNotNull();
        assertThat(result.getResetDate()).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(cache, times(2)).evictIfPresent(any()); // Called twice: login cache and email cache
    }

    @Test
    void createUser_WithNullLangKey_ShouldSetDefaultLanguage() {
        // Given
        AdminUserDTO userDTO = createTestUserDTO();
        userDTO.setId(null);
        userDTO.setLangKey(null);

        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(userAuthority));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertThat(user.getLangKey()).isEqualTo(Constants.DEFAULT_LANGUAGE);
            return user;
        });
        when(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(cache);
        when(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(cache);

        // When
        userService.createUser(userDTO);

        // Then
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_WithValidData_ShouldUpdateUser() {
        // Given
        AdminUserDTO userDTO = createTestUserDTO();
        User existingUser = createTestUser();
        Set<Authority> authorities = new HashSet<>();
        authorities.add(userAuthority);
        existingUser.setAuthorities(authorities);

        when(userRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(existingUser));
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(userAuthority));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(cache);
        when(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(cache);

        // When
        Optional<AdminUserDTO> result = userService.updateUser(userDTO);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getLogin()).isEqualTo(DEFAULT_LOGIN.toLowerCase());
        verify(userRepository).save(any(User.class));
        verify(cache, times(4)).evictIfPresent(any()); // clearUserCaches called twice, each evicts 2 caches
    }

    @Test
    void updateUser_WithNonExistentUser_ShouldReturnEmpty() {
        // Given
        AdminUserDTO userDTO = createTestUserDTO();

        when(userRepository.findById(DEFAULT_ID)).thenReturn(Optional.empty());

        // When
        Optional<AdminUserDTO> result = userService.updateUser(userDTO);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void updateUser_WithCurrentUserLogin_ShouldUpdateCurrentUser() {
        // Given
        String firstName = "UpdatedFirst";
        String lastName = "UpdatedLast";
        String email = "updated@example.com";
        String langKey = "fr";
        String imageUrl = "http://updated.com/image.jpg";

        User currentUser = createTestUser();

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenReturn(currentUser);
            when(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(cache);
            when(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(cache);

            // When
            userService.updateUser(firstName, lastName, email, langKey, imageUrl);

            // Then
            verify(userRepository).save(
                argThat(
                    user ->
                        user.getFirstName().equals(firstName) &&
                        user.getLastName().equals(lastName) &&
                        user.getEmail().equals(email.toLowerCase()) &&
                        user.getLangKey().equals(langKey) &&
                        user.getImageUrl().equals(imageUrl)
                )
            );
            verify(cache).evictIfPresent(DEFAULT_LOGIN);
            verify(cache).evictIfPresent(email.toLowerCase());
        }
    }

    @Test
    void changePassword_WithValidCredentials_ShouldChangePassword() {
        // Given
        String currentPassword = "current-password";
        String newPassword = "new-password";
        String encodedNewPassword = "encoded-new-password";

        User currentUser = createTestUser();
        currentUser.setPassword("encoded-current-password");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(currentUser));
            when(passwordEncoder.matches(currentPassword, "encoded-current-password")).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
            when(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(cache);
            when(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(cache);

            // When
            userService.changePassword(currentPassword, newPassword);

            // Then
            assertThat(currentUser.getPassword()).isEqualTo(encodedNewPassword);
            verify(cache).evictIfPresent(DEFAULT_LOGIN);
            verify(cache).evictIfPresent(DEFAULT_EMAIL);
        }
    }

    @Test
    void changePassword_WithInvalidCurrentPassword_ShouldThrowException() {
        // Given
        String wrongCurrentPassword = "wrong-password";
        String newPassword = "new-password";

        User currentUser = createTestUser();
        currentUser.setPassword("encoded-current-password");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(currentUser));
            when(passwordEncoder.matches(wrongCurrentPassword, "encoded-current-password")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> userService.changePassword(wrongCurrentPassword, newPassword)).isInstanceOf(
                InvalidPasswordException.class
            );
        }
    }

    @Test
    void deleteUser_WithExistingUser_ShouldDeleteUser() {
        // Given
        String login = DEFAULT_LOGIN;
        User userToDelete = createTestUser();

        when(userRepository.findOneByLogin(login)).thenReturn(Optional.of(userToDelete));
        when(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(cache);
        when(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(cache);

        // When
        userService.deleteUser(login);

        // Then
        verify(userRepository).delete(userToDelete);
        verify(cache).evictIfPresent(DEFAULT_LOGIN);
        verify(cache).evictIfPresent(DEFAULT_EMAIL);
    }

    @Test
    void deleteUser_WithNonExistentUser_ShouldDoNothing() {
        // Given
        String nonExistentLogin = "nonexistent";

        when(userRepository.findOneByLogin(nonExistentLogin)).thenReturn(Optional.empty());

        // When
        userService.deleteUser(nonExistentLogin);

        // Then
        verify(userRepository, never()).delete(any());
    }

    @Test
    void getAllManagedUsers_ShouldReturnPageOfUsers() {
        // Given
        Pageable pageable = Pageable.unpaged();
        List<User> users = List.of(testUser);
        Page<User> userPage = new PageImpl<>(users);

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // When
        Page<AdminUserDTO> result = userService.getAllManagedUsers(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLogin()).isEqualTo(DEFAULT_LOGIN);
    }

    @Test
    void getAllPublicUsers_ShouldReturnPageOfPublicUsers() {
        // Given
        Pageable pageable = Pageable.unpaged();
        List<User> users = List.of(testUser);
        Page<User> userPage = new PageImpl<>(users);

        when(userRepository.findAllByIdNotNullAndActivatedIsTrue(pageable)).thenReturn(userPage);

        // When
        Page<UserDTO> result = userService.getAllPublicUsers(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLogin()).isEqualTo(DEFAULT_LOGIN);
    }

    @Test
    void getUserWithAuthoritiesByLogin_ShouldReturnUser() {
        // Given
        String login = DEFAULT_LOGIN;

        when(userRepository.findOneWithAuthoritiesByLogin(login)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserWithAuthoritiesByLogin(login);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getLogin()).isEqualTo(DEFAULT_LOGIN);
    }

    @Test
    void getUserWithAuthorities_ShouldReturnCurrentUser() {
        // Given
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userRepository.findOneWithAuthoritiesByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(testUser));

            // When
            Optional<User> result = userService.getUserWithAuthorities();

            // Then
            assertThat(result).isPresent();
            assertThat(result.orElseThrow().getLogin()).isEqualTo(DEFAULT_LOGIN);
        }
    }

    @Test
    void removeNotActivatedUsers_ShouldRemoveOldInactiveUsers() {
        // Given
        User oldInactiveUser = createTestUser();
        oldInactiveUser.setActivated(false);
        oldInactiveUser.setActivationKey("key");
        oldInactiveUser.setCreatedDate(Instant.now().minus(4, ChronoUnit.DAYS));

        List<User> oldUsers = List.of(oldInactiveUser);

        when(userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(any(Instant.class))).thenReturn(
            oldUsers
        );
        when(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(cache);
        when(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(cache);

        // When
        userService.removeNotActivatedUsers();

        // Then
        verify(userRepository).delete(oldInactiveUser);
        verify(cache).evictIfPresent(oldInactiveUser.getLogin());
        verify(cache).evictIfPresent(oldInactiveUser.getEmail());
    }

    @Test
    void getAuthorities_ShouldReturnAllAuthorities() {
        // Given
        Authority adminAuthority = new Authority();
        adminAuthority.setName(AuthoritiesConstants.ADMIN);
        List<Authority> authorities = List.of(userAuthority, adminAuthority);

        when(authorityRepository.findAll()).thenReturn(authorities);

        // When
        List<String> result = userService.getAuthorities();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);
    }
}
