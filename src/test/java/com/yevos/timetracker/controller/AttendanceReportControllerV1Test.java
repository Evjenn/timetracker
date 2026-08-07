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
import com.yevos.timetracker.security.filter.JwtFilter;
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
        // Готовим тестового админа для обхода Spring Security
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
        // 1. Создаем четкие физические объекты сущностей в памяти
        WorkShift realShift = new WorkShift();
        AbsenceRecord realAbsence = new AbsenceRecord();

        // 2. Кладем их внутрь агрегата по прямым ссылкам
        UserAttendanceAggregate mockAggregate = new UserAttendanceAggregate(
                1L,
                "taras_dev",
                List.of(realShift),
                List.of(realAbsence)
        );

        // 3. Создаем DTO-ответы, которыми наполним JSON
        WorkShiftResponse mockShiftDto = new WorkShiftResponse();
        mockShiftDto.setId(555L);

        AbsenceResponse mockAbsenceDto = new AbsenceResponse();
        mockAbsenceDto.setId(777L);

        // 4. Обучаем Mockito жестко реагировать на эти конкретные объекты
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
                // Точечно проверяем первый элемент [0] массива отчетов всей компании
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
        Long expectedUserId = 1L; // Наш залогиненный администратор (principalAdmin)
        String fakeCsvString = "Employee,Shift ID,Date\ntaras_dev,1,2026-07-19\n";

        // Обучаем мок сервиса возвращать заготовленную текстовую строку отчета
        when(reportService.exportShiftsToCsv(eq(expectedUserId), any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class)))
                .thenReturn(fakeCsvString);

        // When & Then
        mockMvc.perform(get("/api/v1/reports/export/csv")
                        .with(user(principalAdmin)) // Проходим под сессией администратора
                        .param("year", "2026")
                        .param("month", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Проверяем, что в заголовках ответа прилетел правильный тип контента для CSV-файла
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    assertNotNull(contentType);
                    assertTrue(contentType.contains("text/csv"));
                });

        // Проверяем, что контроллер действительно обратился к сервису отчётов с правильным ID пользователя
        verify(reportService, times(1)).exportShiftsToCsv(eq(expectedUserId), any(), any());
    }
}
