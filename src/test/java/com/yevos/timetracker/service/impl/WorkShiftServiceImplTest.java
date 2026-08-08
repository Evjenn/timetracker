package com.yevos.timetracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yevos.timetracker.exception.BaseException;
import com.yevos.timetracker.model.entity.UserEntity;
import com.yevos.timetracker.model.entity.WorkShift;
import com.yevos.timetracker.repository.UserRepository;
import com.yevos.timetracker.repository.WorkShiftRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class WorkShiftServiceImplTest {

    @Mock
    private WorkShiftRepository workShiftRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private WorkShiftServiceImpl workShiftService;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("taras_dev");
        testUser.setHourlyRate(BigDecimal.valueOf(20.00));
    }

    @Test
    @DisplayName("Should successfully start a new work shift")
    void startShift_Success() {
        // Given
        when(workShiftRepository.findByUserIdAndEndTimeIsNull(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(workShiftRepository.save(any(WorkShift.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
        // When
        WorkShift createdShift = workShiftService.startShift(1L);
        // Then
        assertNotNull(createdShift);
        assertEquals(testUser, createdShift.getUser());
        assertNotNull(createdShift.getStartTime());
        assertNull(createdShift.getEndTime());
        assertEquals(0, createdShift.getBreakDurationMinutes());
        assertEquals(testUser.getHourlyRate(), createdShift.getRateAtTheTime());

        verify(workShiftRepository, times(1)).save(any(WorkShift.class));
    }

    @Test
    @DisplayName("Should throw exception when starting a shift if active shift already exists")
    void startShift_AlreadyHasActiveShift_ThrowsException() {
        // Given
        WorkShift existingShift = new WorkShift();
        when(workShiftRepository.findByUserIdAndEndTimeIsNull(1L)).thenReturn(Optional.of(existingShift));
        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            workShiftService.startShift(1L);
        });

        assertEquals("Active shift is already exists", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(workShiftRepository, never()).save(any(WorkShift.class));
    }

    @Test
    @DisplayName("Should successfully end work shift and accurately calculate profit minus break duration")
    void endShift_SuccessAndCalculatesProfit() {
        // Given
        WorkShift activeShift = new WorkShift();
        activeShift.setUser(testUser);
        activeShift.setRateAtTheTime(BigDecimal.valueOf(20.00));
        activeShift.setBreakDurationMinutes(45);

        // Simulate shift start time exactly 2 hours and 45 minutes ago (165 minutes total)
        // Clean working time should be exactly 2 hours (165 total - 45 break)
        LocalDateTime fakeStartTime = LocalDateTime.now().minusMinutes(165);
        activeShift.setStartTime(fakeStartTime);

        when(workShiftRepository.findByUserIdAndEndTimeIsNull(1L))
                .thenReturn(Optional.of(activeShift));
        when(workShiftRepository.save(any(WorkShift.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // When
        WorkShift completedShift = workShiftService.endShift(1L);
        // Then
        assertNotNull(completedShift.getEndTime());
        // 2 hours of work * $20/hour = $40.00 expected profit
        BigDecimal expectedProfit = BigDecimal.valueOf(40.00).setScale(2);
        assertEquals(expectedProfit, completedShift.getProfit());

        verify(workShiftRepository, times(1)).save(activeShift);
    }

    @Test
    @DisplayName("Should successfully record current break start time")
    void startBreak_ValidShift_SavesStartTime() {
        // Given
        WorkShift activeShift = new WorkShift();
        activeShift.setId(10L);
        activeShift.setCurrentBreakStartTime(null);

        when(workShiftRepository.findByUserIdAndEndTimeIsNull(1L)).thenReturn(Optional.of(activeShift));
        when(workShiftRepository.save(any(WorkShift.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
        // When
        workShiftService.startBreak(1L);
        // Then
        assertNotNull(activeShift.getCurrentBreakStartTime());
        verify(workShiftRepository, times(1)).save(activeShift);
    }

    @Test
    @DisplayName("Should successfully calculate break duration and update total minutes")
    void endBreak_ValidActiveBreak_AccumulatesMinutesAndClearsStartTime() {
        // Given
        WorkShift activeShift = new WorkShift();
        activeShift.setId(10L);
        activeShift.setBreakDurationMinutes(15);
        activeShift.setCurrentBreakStartTime(LocalDateTime.now().minusMinutes(10));

        when(workShiftRepository.findByUserIdAndEndTimeIsNull(1L)).thenReturn(Optional.of(activeShift));
        when(workShiftRepository.save(any(WorkShift.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
        // When
        workShiftService.endBreak(1L);
        // Then
        assertNull(activeShift.getCurrentBreakStartTime());
        assertTrue(activeShift.getBreakDurationMinutes() >= 25);
        verify(workShiftRepository, times(1)).save(activeShift);
    }

    @Test
    @DisplayName("Should throw BaseException when starting shift if user hourly rate is missing")
    void startShift_UserHasNoHourlyRate_ThrowsBaseException() {
        // Given
        Long userId = 1L;
        UserEntity userWithoutRate = new UserEntity();
        userWithoutRate.setId(userId);
        userWithoutRate.setHourlyRate(null);

        when(workShiftRepository.findByUserIdAndEndTimeIsNull(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithoutRate));
        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            workShiftService.startShift(userId);
        });

        assertEquals("Cannot start shift: User hourly rate must be greater than zero", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(workShiftRepository, never()).save(any(WorkShift.class));
    }

    @Test
    @DisplayName("Should successfully update shift data and recalculate profit by Admin")
    void adminUpdateShift_ValidData_UpdatesFieldsAndRecalculatesProfit() {
        // Given
        Long shiftId = 12L;
        WorkShift existingShift = new WorkShift();
        existingShift.setId(shiftId);
        existingShift.setRateAtTheTime(BigDecimal.valueOf(50.00));
        existingShift.setBreakDurationMinutes(0);

        LocalDateTime newStart = LocalDateTime.of(2026, 7, 19, 10, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 7, 19, 12, 0);

        when(workShiftRepository.findById(shiftId)).thenReturn(Optional.of(existingShift));
        when(workShiftRepository.save(any(WorkShift.class))).thenAnswer(
                inv -> inv.getArgument(0));
        // When
        workShiftService.adminUpdateShift(shiftId, newStart, newEnd, 0, "Fixed by foreman");
        // Then
        assertEquals(newStart, existingShift.getStartTime());
        assertEquals(newEnd, existingShift.getEndTime());
        assertEquals("RESOLVED_BY_ADMIN: Fixed by foreman", existingShift.getStatusNote());
        assertEquals(0, BigDecimal.valueOf(100.00).compareTo(existingShift.getProfit()));
        verify(workShiftRepository, times(1)).save(existingShift);
    }

    @Test
    @DisplayName("getAdminAlerts() should return combined list of auto-closed and forgotten shifts")
    void getAdminAlerts_ExistingAlertsInDatabase_ReturnsCombinedList() {
        // Given
        WorkShift autoClosedShift = new WorkShift();
        autoClosedShift.setId(1L);
        autoClosedShift.setStatusNote("AUTO_CLOSED");

        WorkShift forgottenShift = new WorkShift();
        forgottenShift.setId(2L);
        forgottenShift.setStatusNote("FORGOTTEN_START");

        when(workShiftRepository.findByStatusNoteStartingWith("AUTO_CLOSED"))
                .thenReturn(List.of(autoClosedShift));
        when(workShiftRepository.findByStatusNoteStartingWith("FORGOTTEN_START"))
                .thenReturn(List.of(forgottenShift));
        // When
        List<WorkShift> result = workShiftService.getAdminAlerts();
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(autoClosedShift));
        assertTrue(result.contains(forgottenShift));

        verify(workShiftRepository, times(1)).findByStatusNoteStartingWith("AUTO_CLOSED");
        verify(workShiftRepository, times(1)).findByStatusNoteStartingWith("FORGOTTEN_START");
    }

}
