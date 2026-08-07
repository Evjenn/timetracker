package com.yevos.timetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yevos.timetracker.mapper.WorkShiftMapper;
import com.yevos.timetracker.model.dto.response.WorkShiftResponse;
import com.yevos.timetracker.model.entity.WorkShift;
import com.yevos.timetracker.security.filter.JwtFilter;
import com.yevos.timetracker.security.service.JwtService;
import com.yevos.timetracker.security.service.UserDetailsImpl;
import com.yevos.timetracker.service.WorkShiftService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
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

@WebMvcTest(WorkShiftControllerV1.class)
class WorkShiftControllerV1Test {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private WorkShiftService workShiftService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private WorkShiftMapper workShiftMapper;
    @MockitoBean
    private UserDetailsService userDetailsService;
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
    @DisplayName("POST /api/v1/shifts/start should return 201 Created and success message")
    void startShift_AuthenticatedUser_ReturnsCreatedAndTextMessage() throws Exception {
        // Given
        WorkShift mockShift = new WorkShift();
        when(workShiftService.startShift(eq(1L))).thenReturn(mockShift);

        // When & Then
        mockMvc.perform(post("/api/v1/shifts/start")
                        .with(user(principalUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                //Проверяем, что сервер вернул наше точное текстовое сообщение
                .andExpect(content().string("Work shift started successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/shifts/start should return 401 Unauthorized when user is anonymous")
    void startShift_AnonymousUser_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/shifts/start")
                        .with(csrf()) // Пользователя .with(user(...)) принципиально НЕ добавляем
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // Ожидаем, что Spring Security вернет 401
    }

    @Test
    @DisplayName("POST /api/v1/shifts/end should return 200 OK and success message " +
            "for authenticated user")
    void endShift_AuthenticatedUser_ReturnsOkAndTextMessage() throws Exception {
        // Given
        WorkShift mockShift = new WorkShift(); // Метод сервиса возвращает объект, но контроллер его проигнорирует
        when(workShiftService.endShift(eq(1L))).thenReturn(mockShift);

        // When & Then
        mockMvc.perform(post("/api/v1/shifts/end")
                        .with(user(principalUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Проверяем, что эндпоинт возвращает именно наш новый красивый текст, а не JSON
                .andExpect(content().string("Work shift closed successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/shifts/end should return 401 Unauthorized when user is anonymous")
    void endShift_AnonymousUser_ReturnsUnauthorized() throws Exception {
        // When & Then (Пользователя .with(user(...)) принципиально НЕ передаем)
        mockMvc.perform(post("/api/v1/shifts/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/shifts/history should return 200 OK and list of shifts for date range")
    void getShiftsHistory_ValidDateRange_ReturnsOkAndShiftsList() throws Exception {
        // Given
        WorkShift mockShift = new WorkShift();
        WorkShiftResponse responseDto = new WorkShiftResponse();
        responseDto.setId(10L);
        responseDto.setTotalWorkingTime("08:30");

        // Обучаем сервис принимать любые LocalDateTime границы, которые вычислит контроллер
        when(workShiftService.getShiftsInPeriod(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(mockShift));
        when(workShiftMapper.toResponse(mockShift)).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/shifts/history")
                        .with(user(principalUser)) // Наш залогиненный юзер с ID 1
                        .param("startDate", "2026-07-01") // 🟢 Передаем чистые ISO даты
                        .param("endDate", "2026-07-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].totalWorkingTime").value("08:30"));

        verify(workShiftService, times(1)).getShiftsInPeriod(eq(1L), any(), any());
    }

    @Test
    @DisplayName("POST /api/v1/shifts/break/start should return 200 OK for authenticated user")
    void startBreak_AuthenticatedUser_ReturnsOkAndSuccessMessage() throws Exception {
        // Given
        doNothing().when(workShiftService).startBreak(eq(1L));

        // When & Then
        mockMvc.perform(post("/api/v1/shifts/break/start")
                        .with(user(principalUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Break started successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/shifts/break/start should return 401 Unauthorized for anonymous user")
    void startBreak_AnonymousUser_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/shifts/break/start")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/shifts/break/end should return 200 OK for authenticated user")
    void endBreak_AuthenticatedUser_ReturnsOkAndSuccessMessage() throws Exception {
        // Given
        doNothing().when(workShiftService).endBreak(eq(1L));

        // When & Then
        mockMvc.perform(post("/api/v1/shifts/break/end")
                        .with(user(principalUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Break ended successfully. Total duration updated."));
    }

    @Test
    @DisplayName("POST /api/v1/shifts/break/end should return 401 Unauthorized for anonymous user")
    void endBreak_AnonymousUser_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/shifts/break/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/shifts/admin/alerts should return 200 OK and list of alert shifts")
    void getAdminAlerts_AuthenticatedAdmin_ReturnsOkAndAlertsList() throws Exception {
        // Given
        WorkShift mockShift = new WorkShift();
        WorkShiftResponse responseDto = new WorkShiftResponse();
        responseDto.setId(777L);
        responseDto.setStatusNote("FORGOTTEN_START_ALERT");

        when(workShiftService.getAdminAlerts()).thenReturn(List.of(mockShift));
        when(workShiftMapper.toResponse(mockShift)).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/shifts/admin/alerts")
                        .with(user(principalUser)) // В тесте пропускаем под любым юзером из-за мока JwtFilter
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(777))
                .andExpect(jsonPath("$[0].statusNote").value("FORGOTTEN_START_ALERT"));
    }

    @Test
    @DisplayName("GET /api/v1/shifts/admin/search-shift should return 200 OK and shift details")
    void findShiftByUsernameAndDate_ValidRequest_ReturnsShiftResponse() throws Exception {
        // Given
        WorkShift mockShift = new WorkShift();
        WorkShiftResponse responseDto = new WorkShiftResponse();
        responseDto.setId(999L);

        when(workShiftService.getShiftByUsernameAndDate(eq("taras_dev"), any(), any()))
                .thenReturn(mockShift);
        when(workShiftMapper.toResponse(mockShift)).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/shifts/admin/search-shift")
                        .with(user(principalUser))
                        .param("username", "taras_dev")
                        .param("date", "2026-07-25")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(999));
    }

    @Test
    @DisplayName("PUT /api/v1/shifts/admin/manage-shift/{id} should return 200 OK for Admin adjustment")
    void adminModifyShift_ValidRequest_ReturnsOkMessage() throws Exception {
        // Given
        Long shiftId = 12L;
        doNothing().when(workShiftService).adminUpdateShift(eq(shiftId), any(), any(), anyInt(), anyString());

        // When & Then
        mockMvc.perform(put("/api/v1/shifts/admin/manage-shift/" + shiftId)
                        .with(user(principalUser))
                        .with(csrf())
                        .param("startTime", "2026-07-19T10:00:00")
                        .param("endTime", "2026-07-19T18:00:00")
                        .param("breakMinutes", "45")
                        .param("note", "Forgot to log time")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Shift ID 12 successfully updated by Admin. Note: Forgot to log time"));
    }

}
