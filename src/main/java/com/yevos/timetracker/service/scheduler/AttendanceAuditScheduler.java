package com.yevos.timetracker.service.scheduler;

import com.yevos.timetracker.model.entity.AbsenceRecord;
import com.yevos.timetracker.model.entity.UserEntity;
import com.yevos.timetracker.model.entity.WorkShift;
import com.yevos.timetracker.repository.AbsenceRepository;
import com.yevos.timetracker.repository.UserRepository;
import com.yevos.timetracker.repository.WorkShiftRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cron scheduler component designed for automated attendance auditing.
 * Responsibilities include cascade-closing unsubmitted active work shifts
 * with an administrative penalty flag, as well as auto-generating silent system logs
 * for officially approved absence periods (vacations/sick leaves).
 */
@Component
@Slf4j
public class AttendanceAuditScheduler {

    private final UserRepository userRepository;
    private final WorkShiftRepository workShiftRepository;
    private final AbsenceRepository absenceRepository;

    public AttendanceAuditScheduler(UserRepository userRepository,
                                    WorkShiftRepository workShiftRepository,
                                    AbsenceRepository absenceRepository) {

        this.userRepository = userRepository;
        this.workShiftRepository = workShiftRepository;
        this.absenceRepository = absenceRepository;
    }

    @Scheduled(cron = "0 0 20 * * MON-FRI")
    @Transactional
    public void executeDailyAudit() {

        log.info("Starting daily attendance audit at 20:00...");
        List<UserEntity> users = userRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        for (UserEntity user : users) {
            Optional<WorkShift> activeShiftOpt = workShiftRepository
                    .findByUserIdAndEndTimeIsNull(user.getId());

            if (activeShiftOpt.isPresent()) {
                processForgotToEndShift(activeShiftOpt.get(), user, now);
            } else {
                processEmptyDay(user, today);
            }
        }
        log.info("Daily attendance audit successfully completed.");
    }

    /**
     * Processing logic for employees who forgot to end their shift (and break interval).
     */
    private void processForgotToEndShift(WorkShift shift, UserEntity user, LocalDateTime now) {

        // Cascade-close the break interval if it was left active
        if (shift.getCurrentBreakStartTime() != null) {
            int previousTotalBreaks = shift.getBreakDurationMinutes();
            shift.setBreakDurationMinutes(previousTotalBreaks + 60);
            shift.setCurrentBreakStartTime(null);
            log.warn("User '{}' left active break. Cascade-closed with 60 min limit.",
                    user.getUsername());
        }

        shift.setEndTime(now);
        shift.setStatusNote("AUTO_CLOSED_AT_20_00");
        shift.setProfit(BigDecimal.ZERO);
        workShiftRepository.save(shift);
        log.warn("User '{}' forgot to end shift. Auto-closed with audit flag.", user.getUsername());
    }

    /**
     * Logic for verifying activity and generating alerts
     * for employees who did not show up for work.
     */
    private void processEmptyDay(UserEntity user, LocalDate today) {

        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(23, 59, 59);

        List<WorkShift> todayShifts = workShiftRepository
                .findUserShiftsInPeriod(user.getId(), dayStart, dayEnd);

        if (todayShifts.isEmpty()) {
            // Check if the user has an official approved leave record for today's date
            Optional<AbsenceRecord> absenceOpt = absenceRepository
                    .findUserAbsencesInPeriod(user.getId(), today, today)
                    .stream()
                    .findFirst();
            WorkShift systemShift = buildSystemShift(user, dayStart, dayEnd);

            if (absenceOpt.isPresent()) {
                // User is officially absent! Apply a non-penalizing system log entry
                AbsenceRecord absence = absenceOpt.get();
                systemShift.setStatusNote("APPROVED_ABSENCE: " + absence.getAbsenceType());
                log.info("User '{}' is on official '{}'. Created silent system row.",
                        user.getUsername(), absence.getAbsenceType());
            } else {
                // Truant shift registration (triggers administrative screening alert)
                systemShift.setStatusNote("FORGOTTEN_START_ALERT");
                log.warn("User '{}' did not start any shift today. Created audit alert row.",
                        user.getUsername());
            }
            workShiftRepository.save(systemShift);
        }
    }

    private WorkShift buildSystemShift(
            UserEntity user, LocalDateTime dayStart, LocalDateTime dayEnd) {

        WorkShift shift = new WorkShift();
        shift.setUser(user);
        shift.setStartTime(dayStart);
        shift.setEndTime(dayEnd);
        shift.setRateAtTheTime(
                user.getHourlyRate() != null ? user.getHourlyRate() : BigDecimal.ZERO);
        shift.setBreakDurationMinutes(0);
        shift.setProfit(BigDecimal.ZERO);
        return shift;
    }
}
