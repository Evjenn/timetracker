package com.yevos.timetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yevos.timetracker.mapper.UserMapper;
import com.yevos.timetracker.model.dto.request.UpdatePasswordRequest;
import com.yevos.timetracker.model.dto.request.UpdateRateRequest;
import com.yevos.timetracker.model.dto.request.UpdateUserRequest;
import com.yevos.timetracker.model.dto.response.UserResponse;
import com.yevos.timetracker.model.entity.UserEntity;
import com.yevos.timetracker.security.filter.JwtFilter;
import com.yevos.timetracker.security.service.JwtService;
import com.yevos.timetracker.security.service.UserDetailsImpl;
import com.yevos.timetracker.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserControllerV1.class)
class UserControllerV1Test {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserDetailsImpl principalUser;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());

        principalUser = new UserDetailsImpl(
                1L,
                "taras_dev",
                "encodedPassword",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("PUT /api/v1/users/rate should return 200 OK and mapped UserResponse")
    void updateHourlyRate_ValidRate_ReturnsOkAndDto() throws Exception {
        // Given
        UpdateRateRequest request = new UpdateRateRequest();
        request.setHourlyRate(BigDecimal.valueOf(35.50));

        UserEntity mockUser = new UserEntity();
        UserResponse mockResponse = new UserResponse();
        mockResponse.setId(1L);
        mockResponse.setHourlyRate(BigDecimal.valueOf(35.50));

        // Обучаем моки под новые сигнатуры методов
        when(userService.updateHourlyRate(1L, BigDecimal.valueOf(35.50))).thenReturn(mockUser);
        when(userMapper.toResponse(mockUser)).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/users/rate")
                        .with(user(principalUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hourlyRate").value(35.50))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/users/rate should return 401 Unauthorized when user is anonymous")
    void updateHourlyRate_AnonymousUser_ReturnsUnauthorized() throws Exception {
        // Given
        UpdateRateRequest request = new UpdateRateRequest();
        request.setHourlyRate(BigDecimal.valueOf(35.50));

        // When & Then (No .with(user(...)) provided)
        mockMvc.perform(put("/api/v1/users/rate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/profile should return 200 OK and updated fields")
    void updateProfile_ValidRequest_ReturnsOkAndDto() throws Exception {
        // Given
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("new_taras_dev");
        request.setEmail("new_email@example.com");

        UserEntity mockUser = new UserEntity();
        UserResponse mockResponse = new UserResponse();
        mockResponse.setId(1L);
        mockResponse.setUsername("new_taras_dev");
        mockResponse.setEmail("new_email@example.com");

        when(userService.updateProfile(eq(1L), anyString(), anyString())).thenReturn(mockUser);
        when(userMapper.toResponse(mockUser)).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/profile")
                        .with(user(principalUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_taras_dev"))
                .andExpect(jsonPath("$.email").value("new_email@example.com"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/password should return 200 OK when passwords are valid")
    void updatePassword_ValidRequest_ReturnsOk() throws Exception {
        // Given
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("OldPassword123");
        request.setNewPassword("NewPassword123");

        doNothing().when(userService).updatePassword(eq(1L), any(UpdatePasswordRequest.class));

        // When & Then
        mockMvc.perform(put("/api/v1/users/password")
                        .with(user(principalUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/me should return 204 No Content for authenticated user")
    void deleteAccount_AuthenticatedUser_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(userService).deactivateUser(1L);

        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/users/me")
                        .with(user(principalUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent()); // Ожидаем 204 статус
    }

    @Test
    @DisplayName("DELETE /api/v1/users/me should return 401 Unauthorized for anonymous user")
    void deleteAccount_AnonymousUser_ReturnsUnauthorized() throws Exception {
        // When & Then (Пользователя .with(user(...)) не передаем)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
