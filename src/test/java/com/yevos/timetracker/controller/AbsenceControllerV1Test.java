package com.yevos.timetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yevos.timetracker.mapper.AbsenceMapper;
import com.yevos.timetracker.model.dto.request.AbsenceRequest;
import com.yevos.timetracker.model.dto.response.AbsenceResponse;
import com.yevos.timetracker.model.entity.AbsenceRecord;
import com.yevos.timetracker.model.entity.AbsenceType;
import com.yevos.timetracker.security.config.AuthEntryPointJwt;
import com.yevos.timetracker.security.filter.JwtFilter;
import com.yevos.timetracker.security.service.UserDetailsImpl;
import com.yevos.timetracker.security.service.UserDetailsServiceImpl;
import com.yevos.timetracker.service.AbsenceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AbsenceControllerV1.class)
class AbsenceControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private UserDetailsImpl principalUser;
    private UserDetailsImpl principalAdmin;

    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private AuthEntryPointJwt authEntryPointJwt;
    @MockitoBean
    private UserDetailsServiceImpl userDetailsServiceImpl;
    @MockitoBean
    private AbsenceService absenceService;
    @MockitoBean
    private AbsenceMapper absenceMapper;

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

        principalAdmin = new UserDetailsImpl(
                2L,
                "super_admin",
                "encodedAdminPassword",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    @DisplayName("POST /api/v1/absences/admin should return 201 Created for authenticated Admin")
    void createAbsence_AuthenticatedAdmin_ReturnsCreatedAndDto() throws Exception {
        // Given
        AbsenceRequest request = new AbsenceRequest();
        request.setStartDate(LocalDate.of(2026, 7, 20));
        request.setEndDate(LocalDate.of(2026, 7, 26));
        request.setAbsenceType(AbsenceType.VACATION);
        request.setReason("Rest");

        AbsenceRecord mockRecord = new AbsenceRecord();
        AbsenceResponse mockResponse = new AbsenceResponse();
        mockResponse.setId(55L);
        mockResponse.setAbsenceType(AbsenceType.VACATION);
        mockResponse.setTotalDays(7);

        when(absenceService.createAbsence(eq("taras_dev"), any(AbsenceRequest.class))).thenReturn(mockRecord);
        when(absenceMapper.toResponse(mockRecord)).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/absences/admin")
                        .with(user(principalAdmin))
                        .with(csrf())
                        .param("username", "taras_dev")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.absenceType").value("VACATION"))
                .andExpect(jsonPath("$.totalDays").value(7));

        verify(absenceService, times(1)).createAbsence(eq("taras_dev"), any());
    }

    @Test
    @DisplayName("GET /api/v1/absences/history should return 200 OK and list of absence responses")
    void getAbsenceHistory_ValidPeriod_ReturnsOkAndList() throws Exception {
        // Given
        AbsenceRecord mockRecord = new AbsenceRecord();
        AbsenceResponse mockResponse = new AbsenceResponse();
        mockResponse.setId(56L);
        mockResponse.setAbsenceType(AbsenceType.SICK_LEAVE);

        when(absenceService.getUserAbsences(eq(1L), any(), any())).thenReturn(List.of(mockRecord));
        when(absenceMapper.toResponse(mockRecord)).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/absences/history")
                        .with(user(principalUser))
                        .param("start", "2026-07-01")
                        .param("end", "2026-07-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(56))
                .andExpect(jsonPath("$[0].absenceType").value("SICK_LEAVE"));
    }
}
