package com.yevos.timetracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yevos.timetracker.model.dto.request.LoginRequest;
import com.yevos.timetracker.model.dto.response.AuthResponse;
import com.yevos.timetracker.security.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("login() should return valid token when credentials are correct")
    void login_ValidCredentials_ReturnsAuthResponseWithToken() {
        // given
        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("1234");

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);
        when(jwtService.generateToken(authentication))
                .thenReturn("fake-jwt");
        // when
        AuthResponse response = authService.login(request);
        // then
        assertEquals("fake-jwt", response.getToken());
    }
}