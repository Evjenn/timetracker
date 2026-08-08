package com.yevos.timetracker.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yevos.timetracker.mapper.AbsenceMapper;
import com.yevos.timetracker.mapper.WorkShiftMapper;
import com.yevos.timetracker.model.dto.report.UserAttendanceAggregate;
import com.yevos.timetracker.model.dto.response.AbsenceResponse;
import com.yevos.timetracker.model.dto.response.WorkShiftResponse;
import com.yevos.timetracker.model.entity.AbsenceRecord;
import com.yevos.timetracker.model.entity.WorkShift;
import com.yevos.timetracker.security.service.JwtService;
import com.yevos.timetracker.security.service.UserDetailsImpl;
import com.yevos.timetracker.service.AttendanceReportService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AttendanceReportControllerV1.class)
class AttendanceReportControllerV1Test {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AttendanceReportService reportService;
    @MockitoBean
    private WorkShiftMapper workShiftMapper;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private AbsenceMapper absenceMapper;
    private UserDetailsImpl principalAdmin;

    @BeforeEach
    void setUp() {

        principalAdmin = new UserDetailsImpl(
                1L,
                "super_admin",
                "password",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    @DisplayName("GET /api/v1/reports/admin/company-attendance should return 200 OK and mapped timesheet report")
    void getCompanyAttendanceReport_ValidPeriod_ReturnsMappedReportList() throws Exception {
        // Given
        WorkShift realShift = new WorkShift();
        AbsenceRecord realAbsence = new AbsenceRecord();

        UserAttendanceAggregate mockAggregate = new UserAttendanceAggregate(
                1L,
                "taras_dev",
                List.of(realShift),
                List.of(realAbsence)
        );
        WorkShiftResponse mockShiftDto = new WorkShiftResponse();
        mockShiftDto.setId(555L);
        AbsenceResponse mockAbsenceDto = new AbsenceResponse();
        mockAbsenceDto.setId(777L);

        when(reportService.getCompanyAttendanceData(any(), any())).thenReturn(List.of(mockAggregate));
        when(workShiftMapper.toResponse(realShift)).thenReturn(mockShiftDto);
        when(absenceMapper.toResponse(realAbsence)).thenReturn(mockAbsenceDto);

        // When & Then
        mockMvc.perform(get("/api/v1/reports/admin/company-attendance")
                        .with(user(principalAdmin))
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].username").value("taras_dev"))
                .andExpect(jsonPath("$[0].shifts[0].id").value(555))
                .andExpect(jsonPath("$[0].absences[0].id").value(777));

        verify(reportService, times(1)).getCompanyAttendanceData(any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/reports/export/csv should return 200 OK and CSV file bytes")
    void exportMonthlyReportToCsv_ValidRequest_ReturnsCsvFileBytes() throws Exception {
        // Given
        Long expectedUserId = 1L;
        String fakeCsvString = "Employee,Shift ID,Date\ntaras_dev,1,2026-07-19\n";

        when(reportService.exportShiftsToCsv(eq(expectedUserId), any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class)))
                .thenReturn(fakeCsvString);

        // When & Then
        mockMvc.perform(get("/api/v1/reports/export/csv")
                        .with(user(principalAdmin))
                        .param("year", "2026")
                        .param("month", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    assertNotNull(contentType);
                    assertTrue(contentType.contains("text/csv"));
                });

        verify(reportService, times(1)).exportShiftsToCsv(eq(expectedUserId), any(), any());
    }
}
