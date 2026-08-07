package com.yevos.timetracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yevos.timetracker.mapper.WorkShiftMapper;
import com.yevos.timetracker.model.dto.report.UserAttendanceAggregate;
import com.yevos.timetracker.model.dto.response.UserShortResponse;
import com.yevos.timetracker.model.dto.response.WorkShiftResponse;
import com.yevos.timetracker.model.entity.AbsenceRecord;
import com.yevos.timetracker.model.entity.UserEntity;
import com.yevos.timetracker.model.entity.WorkShift;
import com.yevos.timetracker.repository.AbsenceRepository;
import com.yevos.timetracker.repository.UserRepository;
import com.yevos.timetracker.repository.WorkShiftRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttendanceReportServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkShiftRepository workShiftRepository;
    @Mock
    private WorkShiftMapper workShiftMapper;
    @Mock
    private AbsenceRepository absenceRecordRepository;
    @InjectMocks
    private AttendanceReportServiceImpl reportService;

    @Test
    @DisplayName("getCompanyAttendanceData() should return unified aggregates for all employees")
    void getCompanyAttendanceData_ValidPeriod_ReturnsAggregatedUserData() {
        // Given
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 31, 23, 59);

        UserEntity mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setUsername("taras_dev");

        WorkShift mockShift = new WorkShift();
        mockShift.setId(100L);
        mockShift.setUser(mockUser);

        AbsenceRecord mockAbsence = new AbsenceRecord();
        mockAbsence.setId(200L);
        mockAbsence.setUser(mockUser);

        when(userRepository.findAll()).thenReturn(List.of(mockUser));
        when(workShiftRepository.findAllShiftsInPeriod(start, end)).thenReturn(List.of(mockShift));
        when(absenceRecordRepository.findAllAbsencesInPeriod(start.toLocalDate(), end.toLocalDate()))
                .thenReturn(List.of(mockAbsence));

        // When
        List<UserAttendanceAggregate> result = reportService.getCompanyAttendanceData(start, end);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        UserAttendanceAggregate aggregate = result.get(0);
        assertEquals(1L, aggregate.getUserId());
        assertEquals("taras_dev", aggregate.getUsername());
        assertEquals(1, aggregate.getShifts().size());
        assertEquals(1, aggregate.getAbsences().size());

        verify(userRepository, times(1)).findAll();
        verify(workShiftRepository, times(1)).findAllShiftsInPeriod(start, end);
        verify(absenceRecordRepository, times(1)).findAllAbsencesInPeriod(start.toLocalDate(), end.toLocalDate());
    }

    @Test
    @DisplayName("exportShiftsToCsv() should return valid CSV string with formatted times from DTO")
    void exportShiftsToCsv_ExistingShifts_ReturnsFormattedCsvString() {
        // Given
        Long userId = 1L;
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 31, 23, 59);

        WorkShift shift = new WorkShift();
        shift.setId(10L);
        shift.setStartTime(LocalDateTime.of(2026, 7, 21, 9, 0));
        shift.setEndTime(LocalDateTime.of(2026, 7, 21, 18, 0));
        shift.setRateAtTheTime(BigDecimal.valueOf(100.00));
        shift.setProfit(BigDecimal.valueOf(900.00));

        WorkShiftResponse mockDto = new WorkShiftResponse();
        mockDto.setTotalWorkingTime("09:00");
        mockDto.setClearWorkingTime("09:00");

        UserShortResponse mockUserResponse = new UserShortResponse();
        mockUserResponse.setId(userId);
        mockUserResponse.setUsername("john_doe");

        mockDto.setUser(mockUserResponse);

        when(workShiftRepository.findUserShiftsInPeriod(eq(userId), any(), any())).thenReturn(List.of(shift));
        when(workShiftMapper.toResponse(shift)).thenReturn(mockDto); // 🟢 Обучаем маппер

        // When
        String result = reportService.exportShiftsToCsv(userId, start, end);

        // Then
        assertTrue(result.contains("Employee,Shift ID,Date,Shift Start,Shift End")); // Проверяем шапку
        assertTrue(result.contains("10"));                          // Проверяем, что ID смены (10) записался
        assertTrue(result.contains("100.0"));                       // Проверяем ставку
        assertTrue(result.contains("900.0"));                       // Проверяем прибыль

        verify(workShiftMapper, times(1)).toResponse(shift);
    }

    @Test
    @DisplayName("exportCompanyShiftsToCsv() should return valid formatted CSV string for all company rows")
    void exportCompanyShiftsToCsv_ExistingData_ReturnsCleanCsvString() {
        // Given
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 31, 23, 59);

        UserEntity user = new UserEntity();
        user.setUsername("taras_dev");

        // Тестовая строка реальной работы
        WorkShift workShift = new WorkShift();
        workShift.setId(10L);
        workShift.setUser(user);
        workShift.setStartTime(LocalDateTime.of(2026, 7, 21, 9, 0));
        workShift.setEndTime(LocalDateTime.of(2026, 7, 21, 18, 0));
        workShift.setRateAtTheTime(java.math.BigDecimal.valueOf(100.00));
        workShift.setProfit(java.math.BigDecimal.valueOf(900.00));

        // Тестовая строка отпуска, которую якобы сгенерировал робот
        WorkShift absenceShift = new WorkShift();
        absenceShift.setId(11L);
        absenceShift.setUser(user);
        absenceShift.setStartTime(LocalDateTime.of(2026, 7, 25, 0, 0));
        absenceShift.setStatusNote("APPROVED_ABSENCE: VACATION");
        absenceShift.setRateAtTheTime(java.math.BigDecimal.valueOf(100.00));
        absenceShift.setProfit(java.math.BigDecimal.valueOf(0.00));

        WorkShiftResponse mockDto = new WorkShiftResponse();
        mockDto.setTotalWorkingTime("09:00");
        mockDto.setClearWorkingTime("09:00");

        com.yevos.timetracker.model.dto.response.UserShortResponse userShortDto = new com.yevos.timetracker.model.dto.response.UserShortResponse();
        userShortDto.setUsername("taras_dev");
        mockDto.setUser(userShortDto);

        // Обучаем репозиторий отдавать сразу ОБЕ строки из одной таблицы!
        when(workShiftRepository.findAllShiftsInPeriod(start, end)).thenReturn(List.of(workShift, absenceShift));
        when(workShiftMapper.toResponse(any(WorkShift.class))).thenReturn(mockDto);

        // When
        String result = reportService.exportCompanyShiftsToCsv(start, end);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("taras_dev"));
        assertTrue(result.contains("APPROVED_ABSENCE: VACATION"));

        verify(workShiftRepository, times(1)).findAllShiftsInPeriod(start, end);
    }
}
