package com.yevos.timetracker.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "my-super-secret-key-for-jwt-authentication-123456");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 100000L);
    }

    @Test
    @DisplayName("Should successfully generate token and accurately extract username")
    void generateToken_ValidAuthentication_ReturnsCorrectTokenAndUsername() {
        // Given
        Authentication auth = mock(Authentication.class);
        UserDetailsImpl user = mock(UserDetailsImpl.class);

        when(auth.getPrincipal()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("taras");

        // When
        String token = jwtService.generateToken(auth);

        // Then
        assertNotNull(token);
        assertEquals("taras", jwtService.extractUsername(token));
    }

    @Test
    @DisplayName("Should validate token successfully when token is not expired")
    void isValid_TokenNotExpired_ReturnsTrue() {
        // Given
        Authentication auth = mock(Authentication.class);
        UserDetailsImpl user = mock(UserDetailsImpl.class);

        when(auth.getPrincipal()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("taras");
        when(user.getAuthorities()).thenReturn(Collections.emptyList());

        String token = jwtService.generateToken(auth);

        // When
        boolean isValid = jwtService.isValid(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should safely return false when token expiration time is breached")
    void isValid_TokenIsExpired_ReturnsFalse() {
        // Given
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L); // 1 millisecond lifespan

        Authentication auth = mock(Authentication.class);
        UserDetailsImpl user = mock(UserDetailsImpl.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("taras");
        when(user.getAuthorities()).thenReturn(Collections.emptyList());

        String token = jwtService.generateToken(auth);

        // Artificially sleep for a tiny bit to guarantee expiration breach
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        boolean isValid = jwtService.isValid(token);

        // Then
        assertFalse(isValid); // Asserts that service safely caught exception and returned false
    }
}
