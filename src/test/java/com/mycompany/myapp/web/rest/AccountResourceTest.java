package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.MailService;
import com.mycompany.myapp.service.UserService;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.dto.PasswordChangeDTO;
import com.mycompany.myapp.web.rest.errors.EmailAlreadyUsedException;
import com.mycompany.myapp.web.rest.errors.InvalidPasswordException;
import com.mycompany.myapp.web.rest.vm.KeyAndPasswordVM;
import com.mycompany.myapp.web.rest.vm.ManagedUserVM;
import jakarta.servlet.ServletException;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AccountResourceTest {

    private static final String DEFAULT_LOGIN = "testuser";
    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final String DEFAULT_FIRSTNAME = "John";
    private static final String DEFAULT_LASTNAME = "Doe";
    private static final String DEFAULT_PASSWORD = "password123";
    private static final String DEFAULT_LANGKEY = "en";
    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";
    private static final Long DEFAULT_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private MailService mailService;

    @InjectMocks
    private AccountResource accountResource;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private ManagedUserVM managedUserVM;
    private AdminUserDTO adminUserDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountResource).build();
        objectMapper = new ObjectMapper();
        testUser = createTestUser();
        managedUserVM = createManagedUserVM();
        adminUserDTO = createAdminUserDTO();
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

    private ManagedUserVM createManagedUserVM() {
        ManagedUserVM vm = new ManagedUserVM();
        vm.setLogin(DEFAULT_LOGIN);
        vm.setEmail(DEFAULT_EMAIL);
        vm.setFirstName(DEFAULT_FIRSTNAME);
        vm.setLastName(DEFAULT_LASTNAME);
        vm.setPassword(DEFAULT_PASSWORD);
        vm.setLangKey(DEFAULT_LANGKEY);
        vm.setImageUrl(DEFAULT_IMAGEURL);
        vm.setActivated(true);
        return vm;
    }

    private AdminUserDTO createAdminUserDTO() {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(DEFAULT_ID);
        dto.setLogin(DEFAULT_LOGIN);
        dto.setEmail(DEFAULT_EMAIL);
        dto.setFirstName(DEFAULT_FIRSTNAME);
        dto.setLastName(DEFAULT_LASTNAME);
        dto.setActivated(true);
        dto.setLangKey(DEFAULT_LANGKEY);
        dto.setImageUrl(DEFAULT_IMAGEURL);
        return dto;
    }

    @Test
    void registerAccount_WithValidData_ShouldCreateUser() throws Exception {
        // Given
        when(userService.registerUser(any(AdminUserDTO.class), eq(DEFAULT_PASSWORD))).thenReturn(testUser);
        doNothing().when(mailService).sendActivationEmail(any(User.class));

        // When & Then
        mockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(managedUserVM)))
            .andExpect(status().isCreated());

        verify(userService).registerUser(any(AdminUserDTO.class), eq(DEFAULT_PASSWORD));
        verify(mailService).sendActivationEmail(testUser);
    }

    @Test
    void registerAccount_WithInvalidPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        ManagedUserVM invalidVM = createManagedUserVM();
        invalidVM.setPassword("123"); // Too short password

        // When & Then
        mockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidVM)))
            .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any(), any());
        verify(mailService, never()).sendActivationEmail(any());
    }

    @Test
    void registerAccount_WithEmptyPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        ManagedUserVM invalidVM = createManagedUserVM();
        invalidVM.setPassword(""); // Empty password

        // When & Then
        mockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidVM)))
            .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any(), any());
        verify(mailService, never()).sendActivationEmail(any());
    }

    @Test
    void registerAccount_WithTooLongPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        ManagedUserVM invalidVM = createManagedUserVM();
        invalidVM.setPassword("a".repeat(101)); // Too long password

        // When & Then
        mockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidVM)))
            .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any(), any());
        verify(mailService, never()).sendActivationEmail(any());
    }

    @Test
    void activateAccount_WithValidKey_ShouldActivateUser() throws Exception {
        // Given
        String activationKey = "valid-activation-key";
        when(userService.activateRegistration(activationKey)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/api/activate").param("key", activationKey)).andExpect(status().isOk());

        verify(userService).activateRegistration(activationKey);
    }

    @Test
    void activateAccount_WithInvalidKey_ShouldThrowException() throws Exception {
        // Given
        String invalidKey = "invalid-key";
        when(userService.activateRegistration(invalidKey)).thenReturn(Optional.empty());

        // When & Then - Expect ServletException to be thrown
        try {
            mockMvc.perform(get("/api/activate").param("key", invalidKey));
            fail("Expected ServletException to be thrown");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(ServletException.class);
            ServletException servletException = (ServletException) e;
            assertThat(servletException.getCause())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No user was found for this activation key");
        }

        verify(userService).activateRegistration(invalidKey);
    }

    @Test
    void getAccount_WithAuthenticatedUser_ShouldReturnUserDetails() throws Exception {
        // Given
        when(userService.getUserWithAuthorities()).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc
            .perform(get("/api/account").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.login").value(DEFAULT_LOGIN))
            .andExpect(jsonPath("$.firstName").value(DEFAULT_FIRSTNAME))
            .andExpect(jsonPath("$.lastName").value(DEFAULT_LASTNAME))
            .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
            .andExpect(jsonPath("$.activated").value(true));

        verify(userService).getUserWithAuthorities();
    }

    @Test
    void getAccount_WithNoUser_ShouldThrowException() throws Exception {
        // Given
        when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

        // When & Then - Expect ServletException to be thrown
        try {
            mockMvc.perform(get("/api/account").accept(MediaType.APPLICATION_JSON));
            fail("Expected ServletException to be thrown");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(ServletException.class);
            ServletException servletException = (ServletException) e;
            assertThat(servletException.getCause()).isInstanceOf(RuntimeException.class).hasMessageContaining("User could not be found");
        }

        verify(userService).getUserWithAuthorities();
    }

    @Test
    void saveAccount_WithValidData_ShouldUpdateAccount() throws Exception {
        // Given
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.empty());
            when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(testUser));
            doNothing().when(userService).updateUser(anyString(), anyString(), anyString(), anyString(), anyString());

            // When & Then
            mockMvc
                .perform(
                    post("/api/account").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(adminUserDTO))
                )
                .andExpect(status().isOk());

            verify(userService).updateUser(DEFAULT_FIRSTNAME, DEFAULT_LASTNAME, DEFAULT_EMAIL, DEFAULT_LANGKEY, DEFAULT_IMAGEURL);
        }
    }

    @Test
    void saveAccount_WithExistingEmail_ShouldReturnBadRequest() throws Exception {
        // Given
        User anotherUser = createTestUser();
        anotherUser.setLogin("anotheruser");
        anotherUser.setEmail(DEFAULT_EMAIL);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.of(anotherUser));

            // When & Then
            mockMvc
                .perform(
                    post("/api/account").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(adminUserDTO))
                )
                .andExpect(status().isBadRequest());

            verify(userService, never()).updateUser(anyString(), anyString(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void saveAccount_WithNoCurrentUser_ShouldThrowException() throws Exception {
        // Given
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.empty());

            // When & Then - Expect ServletException to be thrown
            try {
                mockMvc.perform(
                    post("/api/account").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(adminUserDTO))
                );
                fail("Expected ServletException to be thrown");
            } catch (Exception e) {
                assertThat(e).isInstanceOf(ServletException.class);
                ServletException servletException = (ServletException) e;
                assertThat(servletException.getCause())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Current user login not found");
            }

            verify(userService, never()).updateUser(anyString(), anyString(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void saveAccount_WithUserNotFound_ShouldThrowException() throws Exception {
        // Given
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.empty());
            when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.empty());

            // When & Then - Expect ServletException to be thrown
            try {
                mockMvc.perform(
                    post("/api/account").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(adminUserDTO))
                );
                fail("Expected ServletException to be thrown");
            } catch (Exception e) {
                assertThat(e).isInstanceOf(ServletException.class);
                ServletException servletException = (ServletException) e;
                assertThat(servletException.getCause())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User could not be found");
            }

            verify(userService, never()).updateUser(anyString(), anyString(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void changePassword_WithValidData_ShouldChangePassword() throws Exception {
        // Given
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setCurrentPassword("oldpassword");
        passwordChangeDTO.setNewPassword("newpassword123");

        doNothing().when(userService).changePassword(anyString(), anyString());

        // When & Then
        mockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(passwordChangeDTO))
            )
            .andExpect(status().isOk());

        verify(userService).changePassword("oldpassword", "newpassword123");
    }

    @Test
    void changePassword_WithInvalidNewPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setCurrentPassword("oldpassword");
        passwordChangeDTO.setNewPassword("123"); // Too short

        // When & Then
        mockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(passwordChangeDTO))
            )
            .andExpect(status().isBadRequest());

        verify(userService, never()).changePassword(anyString(), anyString());
    }

    @Test
    void changePassword_WithEmptyNewPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setCurrentPassword("oldpassword");
        passwordChangeDTO.setNewPassword(""); // Empty password

        // When & Then
        mockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(passwordChangeDTO))
            )
            .andExpect(status().isBadRequest());

        verify(userService, never()).changePassword(anyString(), anyString());
    }

    @Test
    void requestPasswordReset_WithValidEmail_ShouldSendResetEmail() throws Exception {
        // Given
        String email = DEFAULT_EMAIL;
        when(userService.requestPasswordReset(anyString())).thenReturn(Optional.of(testUser));
        doNothing().when(mailService).sendPasswordResetMail(any(User.class));

        // When & Then
        mockMvc
            .perform(post("/api/account/reset-password/init").contentType(MediaType.APPLICATION_JSON).content("\"" + email + "\""))
            .andExpect(status().isOk());

        verify(userService).requestPasswordReset(anyString());
        verify(mailService).sendPasswordResetMail(testUser);
    }

    @Test
    void requestPasswordReset_WithInvalidEmail_ShouldNotSendEmail() throws Exception {
        // Given
        String invalidEmail = "invalid@example.com";
        when(userService.requestPasswordReset(anyString())).thenReturn(Optional.empty());

        // When & Then
        mockMvc
            .perform(post("/api/account/reset-password/init").contentType(MediaType.APPLICATION_JSON).content("\"" + invalidEmail + "\""))
            .andExpect(status().isOk()); // Still returns OK for security reasons

        verify(userService).requestPasswordReset(anyString());
        verify(mailService, never()).sendPasswordResetMail(any());
    }

    @Test
    void finishPasswordReset_WithValidKeyAndPassword_ShouldResetPassword() throws Exception {
        // Given
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("valid-reset-key");
        keyAndPassword.setNewPassword("newpassword123");

        when(userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey())).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(keyAndPassword))
            )
            .andExpect(status().isOk());

        verify(userService).completePasswordReset("newpassword123", "valid-reset-key");
    }

    @Test
    void finishPasswordReset_WithInvalidKey_ShouldThrowException() throws Exception {
        // Given
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("invalid-key");
        keyAndPassword.setNewPassword("newpassword123");

        when(userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey())).thenReturn(Optional.empty());

        // When & Then - Expect ServletException to be thrown
        try {
            mockMvc.perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(keyAndPassword))
            );
            fail("Expected ServletException to be thrown");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(ServletException.class);
            ServletException servletException = (ServletException) e;
            assertThat(servletException.getCause())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No user was found for this reset key");
        }

        verify(userService).completePasswordReset("newpassword123", "invalid-key");
    }

    @Test
    void finishPasswordReset_WithInvalidPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("valid-key");
        keyAndPassword.setNewPassword("123"); // Too short

        // When & Then
        mockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(keyAndPassword))
            )
            .andExpect(status().isBadRequest());

        verify(userService, never()).completePasswordReset(anyString(), anyString());
    }

    @Test
    void finishPasswordReset_WithEmptyPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("valid-key");
        keyAndPassword.setNewPassword(""); // Empty password

        // When & Then
        mockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(keyAndPassword))
            )
            .andExpect(status().isBadRequest());

        verify(userService, never()).completePasswordReset(anyString(), anyString());
    }
}
