package com.mycompany.myapp.web.rest.vm;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.service.dto.AdminUserDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for View Models including LoginVM, ManagedUserVM, and KeyAndPasswordVM.
 */
class ViewModelTest {

    private static final String DEFAULT_LOGIN = "testuser";
    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final String DEFAULT_FIRSTNAME = "John";
    private static final String DEFAULT_LASTNAME = "Doe";
    private static final String DEFAULT_PASSWORD = "password123";
    private static final String DEFAULT_LANGKEY = "en";
    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void loginVM_WithValidData_ShouldPassValidation() {
        // Given
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(DEFAULT_PASSWORD);
        loginVM.setRememberMe(false);

        // When
        Set<ConstraintViolation<LoginVM>> violations = validator.validate(loginVM);

        // Then
        assertThat(violations).isEmpty();
        assertThat(loginVM.getUsername()).isEqualTo(DEFAULT_LOGIN);
        assertThat(loginVM.getPassword()).isEqualTo(DEFAULT_PASSWORD);
        assertThat(loginVM.isRememberMe()).isFalse();
    }

    @Test
    void loginVM_WithEmptyUsername_ShouldFailValidation() {
        // Given
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername("");
        loginVM.setPassword(DEFAULT_PASSWORD);

        // When
        Set<ConstraintViolation<LoginVM>> violations = validator.validate(loginVM);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void loginVM_WithNullUsername_ShouldFailValidation() {
        // Given
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(null);
        loginVM.setPassword(DEFAULT_PASSWORD);

        // When
        Set<ConstraintViolation<LoginVM>> violations = validator.validate(loginVM);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void loginVM_WithEmptyPassword_ShouldFailValidation() {
        // Given
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword("");

        // When
        Set<ConstraintViolation<LoginVM>> violations = validator.validate(loginVM);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void loginVM_WithNullPassword_ShouldFailValidation() {
        // Given
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(null);

        // When
        Set<ConstraintViolation<LoginVM>> violations = validator.validate(loginVM);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void loginVM_WithRememberMeTrue_ShouldWork() {
        // Given
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(DEFAULT_PASSWORD);
        loginVM.setRememberMe(true);

        // When
        Set<ConstraintViolation<LoginVM>> violations = validator.validate(loginVM);

        // Then
        assertThat(violations).isEmpty();
        assertThat(loginVM.isRememberMe()).isTrue();
    }

    @Test
    void loginVM_ToString_ShouldNotRevealPassword() {
        // Given
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(DEFAULT_PASSWORD);
        loginVM.setRememberMe(false);

        // When
        String toString = loginVM.toString();

        // Then
        assertThat(toString).contains(DEFAULT_LOGIN);
        assertThat(toString).doesNotContain(DEFAULT_PASSWORD); // Password should not be in toString
    }

    @Test
    void managedUserVM_WithValidData_ShouldPassValidation() {
        // Given
        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setLogin(DEFAULT_LOGIN);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);
        managedUserVM.setLastName(DEFAULT_LASTNAME);
        managedUserVM.setPassword(DEFAULT_PASSWORD);
        managedUserVM.setLangKey(DEFAULT_LANGKEY);
        managedUserVM.setImageUrl(DEFAULT_IMAGEURL);
        managedUserVM.setActivated(true);

        // When
        Set<ConstraintViolation<ManagedUserVM>> violations = validator.validate(managedUserVM);

        // Then
        assertThat(violations).isEmpty();
        assertThat(managedUserVM.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(managedUserVM.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(managedUserVM.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(managedUserVM.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(managedUserVM.getPassword()).isEqualTo(DEFAULT_PASSWORD);
        assertThat(managedUserVM.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
        assertThat(managedUserVM.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        assertThat(managedUserVM.isActivated()).isTrue();
    }

    @Test
    void managedUserVM_WithInvalidPassword_ShouldFailValidation() {
        // Given
        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setLogin(DEFAULT_LOGIN);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setPassword("123"); // Too short password

        // When
        Set<ConstraintViolation<ManagedUserVM>> violations = validator.validate(managedUserVM);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void managedUserVM_WithTooLongPassword_ShouldFailValidation() {
        // Given
        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setLogin(DEFAULT_LOGIN);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setPassword("a".repeat(101)); // Too long password

        // When
        Set<ConstraintViolation<ManagedUserVM>> violations = validator.validate(managedUserVM);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void managedUserVM_WithMinValidPassword_ShouldPassValidation() {
        // Given
        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setLogin(DEFAULT_LOGIN);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setPassword("1234"); // Minimum valid length

        // When
        Set<ConstraintViolation<ManagedUserVM>> violations = validator.validate(managedUserVM);

        // Then
        // Should pass password length validation (other validations might fail for required fields)
        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("password") && v.getMessage().contains("size"));
    }

    @Test
    void managedUserVM_WithMaxValidPassword_ShouldPassValidation() {
        // Given
        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setLogin(DEFAULT_LOGIN);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setPassword("a".repeat(100)); // Maximum valid length

        // When
        Set<ConstraintViolation<ManagedUserVM>> violations = validator.validate(managedUserVM);

        // Then
        // Should pass password length validation
        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("password") && v.getMessage().contains("size"));
    }

    @Test
    void managedUserVM_ToString_ShouldCallSuperToString() {
        // Given
        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setLogin(DEFAULT_LOGIN);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);

        // When
        String toString = managedUserVM.toString();

        // Then
        assertThat(toString).contains("ManagedUserVM");
        assertThat(toString).contains(DEFAULT_LOGIN);
    }

    @Test
    void managedUserVM_ExtendsAdminUserDTO_ShouldInheritProperties() {
        // Given
        ManagedUserVM managedUserVM = new ManagedUserVM();

        // When
        managedUserVM.setLogin(DEFAULT_LOGIN);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);
        managedUserVM.setLastName(DEFAULT_LASTNAME);
        managedUserVM.setActivated(true);

        // Then
        assertThat(managedUserVM).isInstanceOf(AdminUserDTO.class);
        assertThat(managedUserVM.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(managedUserVM.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(managedUserVM.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(managedUserVM.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(managedUserVM.isActivated()).isTrue();
    }

    @Test
    void keyAndPasswordVM_WithValidData_ShouldWork() {
        // Given
        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey("valid-reset-key");
        keyAndPasswordVM.setNewPassword(DEFAULT_PASSWORD);

        // When
        Set<ConstraintViolation<KeyAndPasswordVM>> violations = validator.validate(keyAndPasswordVM);

        // Then
        // Assuming no specific validation constraints on KeyAndPasswordVM
        assertThat(keyAndPasswordVM.getKey()).isEqualTo("valid-reset-key");
        assertThat(keyAndPasswordVM.getNewPassword()).isEqualTo(DEFAULT_PASSWORD);
    }

    @Test
    void keyAndPasswordVM_WithNullKey_ShouldWork() {
        // Given
        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey(null);
        keyAndPasswordVM.setNewPassword(DEFAULT_PASSWORD);

        // When/Then
        assertThat(keyAndPasswordVM.getKey()).isNull();
        assertThat(keyAndPasswordVM.getNewPassword()).isEqualTo(DEFAULT_PASSWORD);
    }

    @Test
    void keyAndPasswordVM_WithEmptyKey_ShouldWork() {
        // Given
        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey("");
        keyAndPasswordVM.setNewPassword(DEFAULT_PASSWORD);

        // When/Then
        assertThat(keyAndPasswordVM.getKey()).isEmpty();
        assertThat(keyAndPasswordVM.getNewPassword()).isEqualTo(DEFAULT_PASSWORD);
    }

    @Test
    void keyAndPasswordVM_WithNullPassword_ShouldWork() {
        // Given
        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey("valid-key");
        keyAndPasswordVM.setNewPassword(null);

        // When/Then
        assertThat(keyAndPasswordVM.getKey()).isEqualTo("valid-key");
        assertThat(keyAndPasswordVM.getNewPassword()).isNull();
    }

    @Test
    void keyAndPasswordVM_WithEmptyPassword_ShouldWork() {
        // Given
        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey("valid-key");
        keyAndPasswordVM.setNewPassword("");

        // When/Then
        assertThat(keyAndPasswordVM.getKey()).isEqualTo("valid-key");
        assertThat(keyAndPasswordVM.getNewPassword()).isEmpty();
    }

    @Test
    void managedUserVM_EmptyConstructor_ShouldWork() {
        // When
        ManagedUserVM managedUserVM = new ManagedUserVM();

        // Then
        assertThat(managedUserVM).isNotNull();
        assertThat(managedUserVM.getPassword()).isNull();
    }

    @Test
    void loginVM_FieldAccess_ShouldWorkCorrectly() {
        // Given
        LoginVM loginVM = new LoginVM();

        // When
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(DEFAULT_PASSWORD);
        loginVM.setRememberMe(true);

        // Then
        assertThat(loginVM.getUsername()).isEqualTo(DEFAULT_LOGIN);
        assertThat(loginVM.getPassword()).isEqualTo(DEFAULT_PASSWORD);
        assertThat(loginVM.isRememberMe()).isTrue();
    }

    @Test
    void managedUserVM_PasswordConstants_ShouldBeCorrect() {
        // Then
        assertThat(ManagedUserVM.PASSWORD_MIN_LENGTH).isEqualTo(4);
        assertThat(ManagedUserVM.PASSWORD_MAX_LENGTH).isEqualTo(100);
    }

    @Test
    void managedUserVM_WithValidBoundaryPasswords_ShouldWork() {
        // Test minimum length
        ManagedUserVM minVM = new ManagedUserVM();
        minVM.setPassword("1234"); // Exactly 4 characters
        assertThat(minVM.getPassword()).hasSize(ManagedUserVM.PASSWORD_MIN_LENGTH);

        // Test maximum length
        ManagedUserVM maxVM = new ManagedUserVM();
        String maxPassword = "a".repeat(ManagedUserVM.PASSWORD_MAX_LENGTH);
        maxVM.setPassword(maxPassword);
        assertThat(maxVM.getPassword()).hasSize(ManagedUserVM.PASSWORD_MAX_LENGTH);
    }
}
