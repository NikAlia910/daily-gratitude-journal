package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.web.rest.vm.LoginVM;
import jakarta.servlet.ServletException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthenticateControllerTest {

    private static final String DEFAULT_USERNAME = "testuser";
    private static final String DEFAULT_PASSWORD = "password";
    private static final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private AuthenticationManagerBuilder authenticationManagerBuilder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticateController authenticateController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private LoginVM loginVM;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticateController).build();
        objectMapper = new ObjectMapper();
        loginVM = createLoginVM();

        // Set up token validity for JWT generation
        ReflectionTestUtils.setField(authenticateController, "tokenValidityInSeconds", 86400L);
        ReflectionTestUtils.setField(authenticateController, "tokenValidityInSecondsForRememberMe", 2592000L);
    }

    private LoginVM createLoginVM() {
        LoginVM vm = new LoginVM();
        vm.setUsername(DEFAULT_USERNAME);
        vm.setPassword(DEFAULT_PASSWORD);
        vm.setRememberMe(false);
        return vm;
    }

    @Test
    void authorize_WithValidCredentials_ShouldReturnJwtToken() throws Exception {
        // Given
        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn(DEFAULT_USERNAME);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(createMockJwt());

        // When & Then
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(loginVM)))
            .andExpect(status().isOk())
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer " + JWT_TOKEN))
            .andExpect(jsonPath("$.id_token").value(JWT_TOKEN));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void authorize_WithRememberMe_ShouldReturnToken() throws Exception {
        // Given
        loginVM.setRememberMe(true);

        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn(DEFAULT_USERNAME);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(createMockJwt());

        // When & Then
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(loginVM)))
            .andExpect(status().isOk())
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(jsonPath("$.id_token").value(JWT_TOKEN));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void authorize_WithInvalidCredentials_ShouldThrowException() throws Exception {
        // Given
        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(
            new BadCredentialsException("Bad credentials")
        );

        // When & Then - Expect ServletException to be thrown
        try {
            mockMvc.perform(
                post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(loginVM))
            );
            fail("Expected ServletException to be thrown");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(ServletException.class);
            ServletException servletException = (ServletException) e;
            assertThat(servletException.getCause()).isInstanceOf(BadCredentialsException.class).hasMessageContaining("Bad credentials");
        }

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void authorize_WithDisabledAccount_ShouldThrowException() throws Exception {
        // Given
        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(
            new DisabledException("Account disabled")
        );

        // When & Then - Expect ServletException to be thrown
        try {
            mockMvc.perform(
                post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(loginVM))
            );
            fail("Expected ServletException to be thrown");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(ServletException.class);
            ServletException servletException = (ServletException) e;
            assertThat(servletException.getCause()).isInstanceOf(DisabledException.class).hasMessageContaining("Account disabled");
        }

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void authorize_WithEmptyUsername_ShouldReturnBadRequest() throws Exception {
        // Given
        loginVM.setUsername("");

        // When & Then
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(loginVM)))
            .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void authorize_WithNullUsername_ShouldReturnBadRequest() throws Exception {
        // Given
        loginVM.setUsername(null);

        // When & Then
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(loginVM)))
            .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void authorize_WithEmptyPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        loginVM.setPassword("");

        // When & Then
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(loginVM)))
            .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void authorize_WithNullPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        loginVM.setPassword(null);

        // When & Then
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(loginVM)))
            .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void authorize_WithInvalidJsonFormat_ShouldReturnBadRequest() throws Exception {
        // Given
        String invalidJson = "{ invalid json }";

        // When & Then
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
            .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void authorize_ShouldCreateCorrectAuthenticationToken() throws Exception {
        // Given
        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn(DEFAULT_USERNAME);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(createMockJwt());

        // When
        mockMvc.perform(
            post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(loginVM))
        );

        // Then
        verify(authenticationManager).authenticate(
            argThat(
                token ->
                    token instanceof UsernamePasswordAuthenticationToken &&
                    token.getPrincipal().equals(DEFAULT_USERNAME) &&
                    token.getCredentials().equals(DEFAULT_PASSWORD)
            )
        );
    }

    @Test
    void jwtToken_InnerClass_ShouldHaveCorrectStructure() {
        // Given
        String tokenValue = "test-token";

        // When
        AuthenticateController.JWTToken jwtToken = new AuthenticateController.JWTToken(tokenValue);

        // Then
        assertThat(jwtToken.getIdToken()).isEqualTo(tokenValue);
    }

    @Test
    void authorize_ShouldSetBearerTokenInHeader() throws Exception {
        // Given
        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn(DEFAULT_USERNAME);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(createMockJwt());

        // When & Then
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(loginVM)))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer " + JWT_TOKEN));
    }

    private Jwt createMockJwt() {
        return Jwt.withTokenValue(JWT_TOKEN)
            .header("alg", "HS256")
            .claim("sub", DEFAULT_USERNAME)
            .claim("iat", Instant.now())
            .claim("exp", Instant.now().plusSeconds(86400))
            .build();
    }
}
