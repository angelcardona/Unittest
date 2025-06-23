package com.tallercarpro.appTaller.service;

import com.tallercarpro.appTaller.dto.LoginRequest;
import com.tallercarpro.appTaller.dto.JwtResponse;
import com.tallercarpro.appTaller.dto.RegisterRequest;
import com.tallercarpro.appTaller.model.User;
import com.tallercarpro.appTaller.repository.UserRepository;
import com.tallercarpro.appTaller.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword"); // Password already encoded
        testUser.setRoles("ROLE_USER"); // Example role
    }

    @Test
    void authenticateUser_shouldReturnJwtResponseOnSuccess() {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "rawPassword");
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class); // Mocking UserDetails returned by UserDetailsService

        when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser"); // Ensure username is correct for JWT generation
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("mockJwtToken");

        // When
        JwtResponse response = authService.authenticateUser(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mockJwtToken");
        assertThat(response.getUsername()).isEqualTo("testuser");
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, times(1)).generateToken(any(UserDetails.class));
    }

    @Test
    void authenticateUser_shouldThrowBadCredentialsExceptionOnInvalidCredentials() {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "wrongPassword");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThrows(BadCredentialsException.class, () -> authService.authenticateUser(loginRequest));
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, never()).generateToken(any(UserDetails.class)); // Should not generate token
    }

    @Test
    void registerUser_shouldRegisterNewUserSuccessfully() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest("newUser", "newuser@example.com", "newPassword");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser); // Return a saved user (can be the mock one)

        // When
        String result = authService.registerUser(registerRequest);

        // Then
        assertThat(result).isEqualTo("User registered successfully!");
        verify(userRepository, times(1)).existsByUsername("newUser");
        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(passwordEncoder, times(1)).encode("newPassword");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_shouldThrowExceptionIfUsernameExists() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest("testuser", "newuser@example.com", "newPassword");
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // When & Then
        // Asumiendo que tienes una excepción personalizada como UsernameAlreadyExistsException
        // o que tu servicio lanza RuntimeException o IllegalStateException.
        // Aquí usaré IllegalStateException como ejemplo.
        assertThrows(IllegalStateException.class, () -> authService.registerUser(registerRequest));
        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, never()).existsByEmail(anyString()); // Email check should not be called
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_shouldThrowExceptionIfEmailExists() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest("newUser", "test@example.com", "newPassword");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        assertThrows(IllegalStateException.class, () -> authService.registerUser(registerRequest));
        verify(userRepository, times(1)).existsByUsername("newUser");
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }
}