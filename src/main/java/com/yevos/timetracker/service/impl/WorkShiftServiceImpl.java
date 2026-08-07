package com.yevos.timetracker.service.impl;

import com.yevos.timetracker.exception.BaseException;
import com.yevos.timetracker.model.entity.UserEntity;
import com.yevos.timetracker.model.entity.WorkShift;
import com.yevos.timetracker.repository.UserRepository;
import com.yevos.timetracker.repository.WorkShiftRepository;
import com.yevos.timetracker.service.WorkShiftService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkShiftServiceImpl implements WorkShiftService {

    private final WorkShiftRepository workShiftRepository;
    private final UserRepository userRepository;

    public WorkShiftServiceImpl(WorkShiftRepository workShiftRepository,
                                UserRepository userRepository) {
        this.workShiftRepository = workShiftRepository;
        this.userRepository = userRepository;
    }

    // 1. НАЧАТЬ СМЕНУ
    @Override
    @Transactional
    public WorkShift startShift(Long userId) {
        // Проверяем, нет ли уже открытой смены у этого пользователя
        workShiftRepository.findByUserIdAndEndTimeIsNull(userId).ifPresent(shift -> {
            throw new BaseException("Active shift is already exists", HttpStatus.BAD_REQUEST);
        });

        // Находим пользователя, чтобы забрать его текущую почасовую ставку
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException("User is not found",
                        HttpStatus.NOT_FOUND));
        if (user.getHourlyRate() == null || user.getHourlyRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BaseException(
                    "Cannot start shift: User hourly rate must be greater than zero",
                    HttpStatus.BAD_REQUEST);
        }

        WorkShift shift = new WorkShift();
        shift.setUser(user);
        shift.setStartTime(LocalDateTime.now());
        shift.setRateAtTheTime(user.getHourlyRate()); // Фиксируем ставку на момент начала смены
        shift.setBreakDurationMinutes(0); // Инициализируем паузу нулем

        return workShiftRepository.save(shift);
    }

    // 3. ЗАВЕРШИТЬ СМЕНУ И ПОСЧИТАТЬ ЗАРАБОТОК
    @Override
    @Transactional
    public WorkShift endShift(Long userId) {
        WorkShift activeShift = workShiftRepository.findByUserIdAndEndTimeIsNull(userId)
                .orElseThrow(() -> new BaseException("Active shift is not found",
                        HttpStatus.NOT_FOUND));

        activeShift.setEndTime(LocalDateTime.now());
        BigDecimal totalProfit = calculateProfit(activeShift);
        activeShift.setProfit(totalProfit);

        return workShiftRepository.save(activeShift);
    }

    private BigDecimal calculateProfit(WorkShift shift) {

        // 1. Запрашиваем чистые часы у соседа
        BigDecimal workingHours = calculateClearWorkingHours(shift);

        // 2. Занимаемся ТОЛЬКО своей обязанностью — умножаем часы на ставку
        return workingHours.multiply(shift.getRateAtTheTime())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateClearWorkingHours(WorkShift shift) {
        // Если смена еще не закрыта (вызвали онлайн-просмотр), считаем до текущего момента
        LocalDateTime end = shift.getEndTime() != null ? shift.getEndTime() : LocalDateTime.now();
        // Считаем общую разницу между началом и концом в минутах
        long totalMinutes = Duration.between(shift.getStartTime(), end).toMinutes();
        // Вычитаем нерабочее время
        long workingMinutes = totalMinutes - shift.getBreakDurationMinutes();
        // Защита от ухода в минус
        if (workingMinutes < 0) {
            workingMinutes = 0;
        }

        // Возвращаем чистые часы в формате BigDecimal для максимальной точности
        return BigDecimal.valueOf(workingMinutes)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkShift> getShiftsInPeriod(Long userId, LocalDateTime start, LocalDateTime end) {
        // Если даты перепутаны местами, можно выбросить ошибку
        if (start.isAfter(end)) {
            throw new BaseException("Start date must be before end date", HttpStatus.BAD_REQUEST);
        }

        return workShiftRepository.findUserShiftsInPeriod(userId, start, end);
    }

    @Override
    @Transactional
    public void startBreak(Long userId) {
        WorkShift activeShift = workShiftRepository.findByUserIdAndEndTimeIsNull(userId)
                .orElseThrow(() -> new BaseException("Active work shift not found",
                        HttpStatus.NOT_FOUND));

        // Защита: нельзя начать перерыв, если он уже идет
        if (activeShift.getCurrentBreakStartTime() != null) {
            throw new BaseException("You are already on a break", HttpStatus.BAD_REQUEST);
        }

        activeShift.setCurrentBreakStartTime(LocalDateTime.now());
        workShiftRepository.save(activeShift);
    }

    @Override
    @Transactional
    public void endBreak(Long userId) {
        // 1. Находим смену
        WorkShift activeShift = workShiftRepository.findByUserIdAndEndTimeIsNull(userId)
                .orElseThrow(() -> new BaseException("Active work shift not found",
                        HttpStatus.NOT_FOUND));

        // 2. Валидируем статус
        if (activeShift.getCurrentBreakStartTime() == null) {
            throw new BaseException("You are not currently on a break", HttpStatus.BAD_REQUEST);
        }

        // 3. Запрашиваем чистые минуты у метода-помощника
        int currentBreak = calculateCurrentBreakMinutes(activeShift.getCurrentBreakStartTime());

        // 4. Обновляем состояние сущности
        int previousTotal = activeShift.getBreakDurationMinutes() != null
                ? activeShift.getBreakDurationMinutes() : 0;
        activeShift.setBreakDurationMinutes(previousTotal + currentBreak);
        activeShift.setCurrentBreakStartTime(null); // Стираем блокнот

        // 5. Сохраняем результат
        workShiftRepository.save(activeShift);
    }

    private int calculateCurrentBreakMinutes(LocalDateTime breakStart) {
        long breakMinutes = Duration.between(breakStart, LocalDateTime.now()).toMinutes();

        // Наша логика округления коротких пауз до 1 минуты
        return breakMinutes == 0 ? 1 : (int) breakMinutes;
    }

    @Override
    @Transactional
    public void adminUpdateShift(Long shiftId, LocalDateTime start,
                                 LocalDateTime end, int breaks, String note) {
        WorkShift shift = workShiftRepository.findById(shiftId)
                .orElseThrow(() -> new BaseException("Shift not found", HttpStatus.NOT_FOUND));

        shift.setStartTime(start);
        shift.setEndTime(end);
        shift.setBreakDurationMinutes(breaks);
        shift.setStatusNote("RESOLVED_BY_ADMIN: " + note);

        // Наш готовый метод calculateProfit заново пересчитает деньги по чистым часам!
        BigDecimal updatedProfit = calculateProfit(shift);
        shift.setProfit(updatedProfit);

        workShiftRepository.save(shift);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkShift> getAdminAlerts() {
        // Вытаскиваем смены с пометками автозакрытия и забытого старта
        List<WorkShift> autoClosed = workShiftRepository
                .findByStatusNoteStartingWith("AUTO_CLOSED");
        List<WorkShift> forgotten = workShiftRepository
                .findByStatusNoteStartingWith("FORGOTTEN_START");

        // Объединяем оба списка в один общий пул для админа
        List<WorkShift> allAlerts = new ArrayList<>();
        allAlerts.addAll(autoClosed);
        allAlerts.addAll(forgotten);

        return allAlerts;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkShift getShiftByUsernameAndDate(
            String username, LocalDateTime start, LocalDateTime end) {
        return workShiftRepository.findByUsernameAndPeriod(username, start, end)
                .orElseThrow(() -> new BaseException(
                        "Work shift not found for user '" + username + "' on this date",
                        HttpStatus.NOT_FOUND));
    }

}

