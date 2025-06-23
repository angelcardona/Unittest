package com.tallercarpro.appTaller.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallercarpro.appTaller.dto.LoginRequest;
import com.tallercarpro.appTaller.dto.JwtResponse;
import com.tallercarpro.appTaller.dto.RegisterRequest;
import com.tallercarpro.appTaller.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void authenticateUser_shouldReturnTokenOnValidCredentials() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        JwtResponse jwtResponse = new JwtResponse("jwt_token", 1L, "testuser", "test@example.com", Collections.singletonList("ROLE_USER"));
        when(authService.authenticateUser(any(LoginRequest.class))).thenReturn(jwtResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_token"))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(authService, times(1)).authenticateUser(any(LoginRequest.class));
    }

    @Test
    void authenticateUser_shouldReturnUnauthorizedOnInvalidCredentials() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "wrongpassword");
        when(authService.authenticateUser(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()); // O 401
    }

    @Test
    void registerUser_shouldReturnOkOnSuccessfulRegistration() throws Exception {
        // Given
        RegisterRequest registerRequest = new RegisterRequest("newUser", "new@example.com", "newPassword");
        when(authService.registerUser(any(RegisterRequest.class))).thenReturn("User registered successfully!");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully!"));

        verify(authService, times(1)).registerUser(any(RegisterRequest.class));
    }

    @Test
    void registerUser_shouldReturnBadRequestIfUsernameExists() throws Exception {
        // Given
        RegisterRequest registerRequest = new RegisterRequest("existingUser", "new@example.com", "newPassword");
        when(authService.registerUser(any(RegisterRequest.class)))
                .thenThrow(new IllegalStateException("Error: Username is already taken!")); // O tu excepción personalizada

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest()); // O 409 Conflict si usas esa respuesta para conflictos
    }

    @Test
    void registerUser_shouldReturnBadRequestIfEmailExists() throws Exception {
        // Given
        RegisterRequest registerRequest = new RegisterRequest("newUser", "existing@example.com", "newPassword");
        when(authService.registerUser(any(RegisterRequest.class)))
                .thenThrow(new IllegalStateException("Error: Email is already in use!")); // O tu excepción personalizada

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }
}