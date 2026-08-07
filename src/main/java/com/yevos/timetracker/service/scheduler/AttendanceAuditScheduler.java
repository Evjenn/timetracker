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
 *  Класс для автозакрытия забытых смен со специальной меткой для админа.
 * Так же для автозаполнения дней пропущенных по уважительной причине.
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

    // 🟢 Робот запускается в 20:00 по будням для аудита уходящего дня
    @Scheduled(cron = "0 0 20 * * MON-FRI")
    @Transactional
    public void executeDailyAudit() {
        log.info("Starting daily attendance audit at 20:00...");

        List<UserEntity> users = userRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // 🟩 Главный метод стал тонким и понятным: просто цикл и вызовы помощников
        for (UserEntity user : users) {
            // Ищем активную (незакрытую) смену пользователя на текущий момент
            Optional<WorkShift> activeShiftOpt = workShiftRepository
                    .findByUserIdAndEndTimeIsNull(user.getId());

            if (activeShiftOpt.isPresent()) {
                // Если смена открыта, вытаскиваем её из коробки через .get()
                // и отправляем на закрытие
                processForgotToEndShift(activeShiftOpt.get(), user, now);
            } else {
                // Если смены нет, передаем управление умному методу проверки пустого дня
                processEmptyDay(user, today);
            }
        }
        log.info("Daily attendance audit successfully completed.");
    }

    // ==========================================
    // ВНУТРЕННИЕ МЕТОДЫ-ПОМОЩНИКИ (ИНКАПСУЛЯЦИЯ ЛОГИКИ)
    // ==========================================

    /**
     * Логика обработки сотрудников, которые забыли закрыть смену (и перерыв)
     */
    private void processForgotToEndShift(WorkShift shift, UserEntity user, LocalDateTime now) {

        // Каскадно закрываем перерыв, если он был брошен активным
        if (shift.getCurrentBreakStartTime() != null) {
            int previousTotalBreaks = shift.getBreakDurationMinutes();
            shift.setBreakDurationMinutes(previousTotalBreaks + 60);
            shift.setCurrentBreakStartTime(null);
            log.warn("User '{}' left active break. Cascade-closed with 60 min limit.",
                    user.getUsername());
        }

        // Принудительно закрываем саму смену концом дня
        shift.setEndTime(now);
        shift.setStatusNote("AUTO_CLOSED_AT_20_00");
        shift.setProfit(BigDecimal.ZERO); // Обнуляем для будущего админского аудита

        workShiftRepository.save(shift);
        log.warn("User '{}' forgot to end shift. Auto-closed with audit flag.", user.getUsername());
    }

    /**
     * Логика проверки и создания алертов для сотрудников, которые вообще не вышли на работу
     */
    private void processEmptyDay(UserEntity user, LocalDate today) {

        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(23, 59, 59);

        List<WorkShift> todayShifts = workShiftRepository
                .findUserShiftsInPeriod(user.getId(), dayStart, dayEnd);

        // Если за день нет ни одной смены — проверяем причину
        if (todayShifts.isEmpty()) {

            // Проверяем, нет ли у юзера официального пропуска на сегодняшнюю дату
            Optional<AbsenceRecord> absenceOpt = absenceRepository
                    .findUserAbsencesInPeriod(user.getId(), today, today)
                    .stream()
                    .findFirst();

            // 🟢 ИСПОЛЬЗУЕМ ФАБРИКУ: Сборка объекта ушла в отдельный понятный метод
            WorkShift systemShift = buildSystemShift(user, dayStart, dayEnd);

            if (absenceOpt.isPresent()) {
                // Юзер официально в отпуске! Добавляем мирную системную пометку
                AbsenceRecord absence = absenceOpt.get();
                systemShift.setStatusNote("APPROVED_ABSENCE: " + absence.getAbsenceType());
                log.info("User '{}' is on official '{}'. Created silent system row.",
                        user.getUsername(), absence.getAbsenceType());
            } else {
                // Оправданий нет — это классический забытый старт (алерт админу)
                systemShift.setStatusNote("FORGOTTEN_START_ALERT");
                log.warn("User '{}' did not start any shift today. Created audit alert row.",
                        user.getUsername());
            }
            workShiftRepository.save(systemShift);
        }
    }

    /**
     * Фабричный метод для сборки пустой смены-заглушки (то, о чем подсказывала IntelliJ IDEA!)
     */
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
