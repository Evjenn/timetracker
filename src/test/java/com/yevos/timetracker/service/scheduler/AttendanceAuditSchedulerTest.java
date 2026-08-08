package com.yevos.timetracker.service.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yevos.timetracker.model.entity.AbsenceRecord;
import com.yevos.timetracker.model.entity.AbsenceType;
import com.yevos.timetracker.model.entity.Role;
import com.yevos.timetracker.model.entity.UserEntity;
import com.yevos.timetracker.model.entity.WorkShift;
import com.yevos.timetracker.repository.AbsenceRepository;
import com.yevos.timetracker.repository.UserRepository;
import com.yevos.timetracker.repository.WorkShiftRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttendanceAuditSchedulerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkShiftRepository workShiftRepository;
    @Mock
    private AbsenceRepository absenceRepository;
    @InjectMocks
    private AttendanceAuditScheduler auditScheduler;
    private UserEntity mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setUsername("taras_dev");
        mockUser.setHourlyRate(BigDecimal.valueOf(100.00));
        mockUser.setRole(Role.USER);
    }

    @Test
    @DisplayName("executeDailyAudit() should cascade-close active break and shift when user forgot to end work")
    void executeDailyAudit_UserForgotToEndShiftAndBreak_CascadeClosesEverything() {
        // Given
        WorkShift forgottenShift = new WorkShift();
        forgottenShift.setId(100L);
        forgottenShift.setUser(mockUser);
        forgottenShift.setCurrentBreakStartTime(LocalDateTime.now().minusHours(2));
        forgottenShift.setBreakDurationMinutes(15);

        when(userRepository.findAll()).thenReturn(List.of(mockUser));
        when(workShiftRepository.findByUserIdAndEndTimeIsNull(mockUser.getId()))
                .thenReturn(Optional.of(forgottenShift));
        // When
        auditScheduler.executeDailyAudit();
        // Then
        assertNull(forgottenShift.getCurrentBreakStartTime());
        assertEquals(75, forgottenShift.getBreakDurationMinutes());
        assertNotNull(forgottenShift.getEndTime());
        assertEquals("AUTO_CLOSED_AT_20_00", forgottenShift.getStatusNote());
        assertEquals(0, BigDecimal.ZERO.compareTo(forgottenShift.getProfit()));

        verify(workShiftRepository, times(1)).save(forgottenShift);
    }

    @Test
    @DisplayName("executeDailyAudit() should create forgotten shift alert when user has no active shift "
            + "and no absence record")
    void executeDailyAudit_UserDidNotStartShiftAndNoVacation_CreatesForgottenStartAlertRow() {
        // Given
        when(userRepository.findAll()).thenReturn(List.of(mockUser));
        when(workShiftRepository.findByUserIdAndEndTimeIsNull(mockUser.getId())).thenReturn(Optional.empty());
        when(workShiftRepository.findUserShiftsInPeriod(eq(mockUser.getId()), any(),
                any())).thenReturn(Collections.emptyList());

        when(absenceRepository.findUserAbsencesInPeriod(eq(mockUser.getId()), any(),
                any())).thenReturn(Collections.emptyList());

        ArgumentCaptor<WorkShift> shiftCaptor = ArgumentCaptor.forClass(WorkShift.class);
        // When
        auditScheduler.executeDailyAudit();
        // Then
        verify(workShiftRepository, times(1)).save(shiftCaptor.capture());
        WorkShift savedShift = shiftCaptor.getValue();

        assertNotNull(savedShift);
        assertEquals("FORGOTTEN_START_ALERT", savedShift.getStatusNote());
        assertEquals(0, BigDecimal.ZERO.compareTo(savedShift.getProfit()));
    }

    @Test
    @DisplayName("executeDailyAudit() should create approved absence row when user did not start shift "
            + "due to official vacation")
    void executeDailyAudit_UserOnOfficialVacation_CreatesSilentApprovedAbsenceRow() {
        // Given
        AbsenceRecord mockVacation = new AbsenceRecord();
        mockVacation.setId(50L);
        mockVacation.setUser(mockUser);
        mockVacation.setAbsenceType(AbsenceType.VACATION);

        when(userRepository.findAll()).thenReturn(List.of(mockUser));
        when(workShiftRepository.findByUserIdAndEndTimeIsNull(mockUser.getId())).thenReturn(Optional.empty());
        when(workShiftRepository.findUserShiftsInPeriod(eq(mockUser.getId()), any(),
                any())).thenReturn(Collections.emptyList());

        when(absenceRepository.findUserAbsencesInPeriod(eq(mockUser.getId()), any(),
                any())).thenReturn(List.of(mockVacation));

        ArgumentCaptor<WorkShift> shiftCaptor = ArgumentCaptor.forClass(WorkShift.class);
        // When
        auditScheduler.executeDailyAudit();
        // Then
        verify(workShiftRepository, times(1)).save(shiftCaptor.capture());
        WorkShift savedShift = shiftCaptor.getValue();

        assertNotNull(savedShift);
        assertEquals("APPROVED_ABSENCE: VACATION", savedShift.getStatusNote());
        assertEquals(0, BigDecimal.ZERO.compareTo(savedShift.getProfit()));
        assertEquals(0, savedShift.getBreakDurationMinutes());
    }
}
