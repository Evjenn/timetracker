package com.yevos.timetracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yevos.timetracker.exception.BaseException;
import com.yevos.timetracker.model.dto.request.AbsenceRequest;
import com.yevos.timetracker.model.entity.AbsenceRecord;
import com.yevos.timetracker.model.entity.AbsenceType;
import com.yevos.timetracker.model.entity.UserEntity;
import com.yevos.timetracker.repository.AbsenceRepository;
import com.yevos.timetracker.repository.UserRepository;
import com.yevos.timetracker.repository.WorkShiftRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceImplTest {

    @Mock
    private AbsenceRepository absenceRepository; // Оставляем ваше имя поля для совместимости с тестами
    @Mock
    private WorkShiftRepository workShiftRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AbsenceServiceImpl absenceService;

    private Long userId;
    private String testUsername; // 🟢 Заменили локальное числовое поле на строковый username
    private UserEntity mockUser;
    private AbsenceRequest defaultRequest;

    @BeforeEach
    void setUp() {
        userId = 1L;
        testUsername = "taras_dev"; // Инициализируем имя для тестов

        mockUser = new UserEntity();
        mockUser.setId(userId);
        mockUser.setUsername(testUsername);

        defaultRequest = new AbsenceRequest();
        defaultRequest.setStartDate(LocalDate.of(2026, 7, 20));
        defaultRequest.setEndDate(LocalDate.of(2026, 7, 26));
        defaultRequest.setAbsenceType(AbsenceType.VACATION);
        defaultRequest.setReason("Summer vacation");
    }

    @Test
    @DisplayName("Should successfully create absence record when all conditions are valid")
    void createAbsence_ValidRequest_ReturnsSavedRecord() {
        // Given
        // 🟢 Переобучили мок искать по уникальному username
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(mockUser));
        when(absenceRepository.findUserAbsencesInPeriod(eq(userId), any(), any())).thenReturn(Collections.emptyList());
        when(workShiftRepository.hasShiftsInPeriod(eq(userId), any(), any())).thenReturn(false);
        when(absenceRepository.save(any(AbsenceRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        // 🟢 Передаем testUsername вместо старого ID
        AbsenceRecord result = absenceService.createAbsence(testUsername, defaultRequest);

        // Then
        assertNotNull(result);
        assertEquals(AbsenceType.VACATION, result.getAbsenceType());
        assertEquals(mockUser, result.getUser());
        assertTrue(result.isApproved()); // Дополнительно проверяем флаг нашего приватного хелпера
        verify(absenceRepository, times(1)).save(any(AbsenceRecord.class));
    }

    @Test
    @DisplayName("Should throw BaseException when start date is after end date")
    void createAbsence_StartDateAfterEndDate_ThrowsBaseException() {
        // Given
        defaultRequest.setStartDate(LocalDate.of(2026, 7, 26));
        defaultRequest.setEndDate(LocalDate.of(2026, 7, 20));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () ->
                absenceService.createAbsence(testUsername, defaultRequest));

        assertEquals("Start date must be before or equal to end date", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    @DisplayName("Should throw BaseException when absence overlaps with another existing absence")
    void createAbsence_OverlappingWithAnotherAbsence_ThrowsBaseException() {
        // Given
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(mockUser));
        when(absenceRepository.findUserAbsencesInPeriod(eq(userId), any(), any()))
                .thenReturn(List.of(new AbsenceRecord()));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () ->
                absenceService.createAbsence(testUsername, defaultRequest));

        // 🟢 Текст ошибки обновлен под каноны нового сервиса
        assertEquals("This employee already has an active absence record inside this period", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    @DisplayName("Should throw BaseException when absence overlaps with actual working shifts")
    void createAbsence_OverlappingWithWorkingShifts_ThrowsBaseException() {
        // Given
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(mockUser));
        when(absenceRepository.findUserAbsencesInPeriod(eq(userId), any(), any())).thenReturn(Collections.emptyList());
        when(workShiftRepository.hasShiftsInPeriod(eq(userId), any(), any())).thenReturn(true);

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () ->
                absenceService.createAbsence(testUsername, defaultRequest));

        // 🟢 Текст ошибки обновлен под каноны нового сервиса
        assertEquals("Cannot register absence: employee has active working shifts within this period.", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }
}
