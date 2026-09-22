package com.quickbite.auth;

import com.quickbite.auth.dto.LoginRequest;
import com.quickbite.auth.dto.RegisterRequest;
import com.quickbite.auth.entity.Role;
import com.quickbite.auth.entity.User;
import com.quickbite.auth.exception.EmailAlreadyExistsException;
import com.quickbite.auth.exception.InvalidCredentialsException;
import com.quickbite.auth.repository.UserRepository;
import com.quickbite.auth.security.JwtService;
import com.quickbite.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the core registration/login logic. These run against
 * Mockito doubles rather than a real database, so they execute fast and
 * exercise the business rules (duplicate email, wrong password) directly.
 */
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_createsUser_whenEmailIsNew() {
        RegisterRequest request = new RegisterRequest("Asha Rao", "asha@example.com", "secret12", "9999999999", Role.CUSTOMER);

        when(userRepository.existsByEmail("asha@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        var response = authService.register(request);

        assertEquals("fake-jwt-token", response.token());
        assertEquals("asha@example.com", response.email());
        assertEquals(Role.CUSTOMER, response.role());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throws_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Asha Rao", "asha@example.com", "secret12", null, Role.CUSTOMER);
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_succeeds_withCorrectPassword() {
        String hashed = passwordEncoder.encode("secret12");
        User existing = User.builder().id(5L).fullName("Asha Rao").email("asha@example.com")
                .passwordHash(hashed).role(Role.CUSTOMER).build();

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(existing)).thenReturn("fake-jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        var response = authService.login(new LoginRequest("asha@example.com", "secret12"));

        assertEquals("fake-jwt-token", response.token());
        assertEquals(5L, response.userId());
    }

    @Test
    void login_throws_withWrongPassword() {
        String hashed = passwordEncoder.encode("secret12");
        User existing = User.builder().id(5L).fullName("Asha Rao").email("asha@example.com")
                .passwordHash(hashed).role(Role.CUSTOMER).build();

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(existing));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("asha@example.com", "wrong-password")));
    }
}
