package com.yevos.timetracker.service.impl;

import com.yevos.timetracker.exception.BaseException;
import com.yevos.timetracker.model.dto.request.AbsenceRequest;
import com.yevos.timetracker.model.entity.AbsenceRecord;
import com.yevos.timetracker.model.entity.UserEntity;
import com.yevos.timetracker.repository.AbsenceRepository;
import com.yevos.timetracker.repository.UserRepository;
import com.yevos.timetracker.repository.WorkShiftRepository;
import com.yevos.timetracker.service.AbsenceService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AbsenceServiceImpl implements AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final UserRepository userRepository;
    private final WorkShiftRepository workShiftRepository;

    public AbsenceServiceImpl(AbsenceRepository absenceRepository,
                              UserRepository userRepository,
                              WorkShiftRepository workShiftRepository) {

        this.absenceRepository = absenceRepository;
        this.userRepository = userRepository;
        this.workShiftRepository = workShiftRepository;
    }

    @Override
    @Transactional
    public AbsenceRecord createAbsence(String username, AbsenceRequest request) {
        // 1. Валидация хронологии дат
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BaseException("Start date must be before or equal to end date",
                    HttpStatus.BAD_REQUEST);
        }

        // 2. Находим пользователя по его уникальному username
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BaseException("User with username '"
                        + username + "' not found", HttpStatus.NOT_FOUND));

        // 3. Защита от наложений: проверяем, нет ли уже других отпусков на этот период
        List<AbsenceRecord> overlapping = absenceRepository.findUserAbsencesInPeriod(
                user.getId(), request.getStartDate(), request.getEndDate()
        );
        if (!overlapping.isEmpty()) {
            throw new BaseException(
                    "This employee already has an active absence record inside this period",
                    HttpStatus.CONFLICT);
        }

        // 4. Проверка наличия живых рабочих смен
        LocalDateTime periodStart = request.getStartDate().atStartOfDay();
        LocalDateTime periodEnd = request.getEndDate().atTime(LocalTime.MAX);

        if (workShiftRepository.hasShiftsInPeriod(user.getId(), periodStart, periodEnd)) {
            throw new BaseException("Cannot register absence: employee has active working shifts "
                    + "within this period.", HttpStatus.CONFLICT);
        }

        // 5. 🟢 ЧИСТЫЙ ВЫЗОВ: Вся рутина сборки полей инкапсулирована в приватном методе ниже!
        AbsenceRecord absenceRecord = fillAbsenceRecordEntity(user, request);

        return absenceRepository.save(absenceRecord);
    }

    private AbsenceRecord fillAbsenceRecordEntity(UserEntity user, AbsenceRequest request) {
        AbsenceRecord absenceRecord = new AbsenceRecord();
        absenceRecord.setUser(user);
        absenceRecord.setStartDate(request.getStartDate());
        absenceRecord.setEndDate(request.getEndDate());
        absenceRecord.setAbsenceType(request.getAbsenceType());
        absenceRecord.setReason(request.getReason());
        absenceRecord.setApproved(true); // Правило: админские пропуски одобряются автоматически
        return absenceRecord;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbsenceRecord> getUserAbsences(Long userId, LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new BaseException("Start date must be before end date", HttpStatus.BAD_REQUEST);
        }
        return absenceRepository.findUserAbsencesInPeriod(userId, start, end);
    }
}
