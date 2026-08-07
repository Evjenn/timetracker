package com.yevos.timetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yevos.timetracker.model.dto.request.LoginRequest;
import com.yevos.timetracker.model.dto.request.RegisterRequest;
import com.yevos.timetracker.model.dto.response.AuthResponse;
import com.yevos.timetracker.security.config.AuthEntryPointJwt;
import com.yevos.timetracker.security.filter.JwtFilter;
import com.yevos.timetracker.security.service.JwtService;
import com.yevos.timetracker.security.service.UserDetailsServiceImpl;
import com.yevos.timetracker.service.AuthService;
import com.yevos.timetracker.service.UserService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthControllerV1.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerV1Test {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean
    private AuthEntryPointJwt authEntryPointJwt;
    @MockitoBean
    private UserDetailsServiceImpl userDetailsServiceImpl;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/v1/auth/register should return 201 Created with empty body")
    void registerUser_ValidRequest_ReturnsCreatedVoid() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new_user");
        request.setPassword("Password123");
        request.setHourlyRate(BigDecimal.valueOf(25.00));
        request.setEmail("user@example.com");

        doNothing().when(userService).registerUser(any(RegisterRequest.class));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").doesNotExist()); // Проверяем, что тело ответа действительно пустое
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 OK and AuthResponse with token")
    void authenticateUser_ValidCredentials_ReturnsAuthResponse() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("taras_dev");
        loginRequest.setPassword("securePassword");

        AuthResponse expectedResponse = new AuthResponse("mocked-jwt-token-string");

        // Настраиваем поведение вашего AuthService
        when(authService.login(any(LoginRequest.class))).thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token-string"));
    }
}
