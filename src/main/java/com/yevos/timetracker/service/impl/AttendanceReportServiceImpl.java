package com.yevos.timetracker.service.impl;

import com.yevos.timetracker.mapper.WorkShiftMapper;
import com.yevos.timetracker.model.dto.report.UserAttendanceAggregate;
import com.yevos.timetracker.model.entity.WorkShift;
import com.yevos.timetracker.repository.AbsenceRepository;
import com.yevos.timetracker.repository.UserRepository;
import com.yevos.timetracker.repository.WorkShiftRepository;
import com.yevos.timetracker.service.AttendanceReportService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceReportServiceImpl implements AttendanceReportService {

    private static final String CSV_HEADER =
            "Employee,Shift ID,Date,Shift Start,Shift End,Break (min),Total Time,"
                    + "Clear Time,Rate,Wage,Note\n";
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");
    private final UserRepository userRepository;
    private final WorkShiftRepository workShiftRepository;
    private final AbsenceRepository absenceRecordRepository;
    private final WorkShiftMapper workShiftMapper;

    public AttendanceReportServiceImpl(UserRepository userRepository,
                                         WorkShiftRepository workShiftRepository,
                                        AbsenceRepository absenceRecordRepository,
                                       WorkShiftMapper workShiftMapper) {
        this.userRepository = userRepository;
        this.workShiftRepository = workShiftRepository;
        this.absenceRecordRepository = absenceRecordRepository;
        this.workShiftMapper = workShiftMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAttendanceAggregate> getCompanyAttendanceData(
            LocalDateTime start, LocalDateTime end) {
        var allUsers = userRepository.findAll();
        var allShifts = workShiftRepository.findAllShiftsInPeriod(start, end);
        var allAbsences = absenceRecordRepository.findAllAbsencesInPeriod(start.toLocalDate(),
                end.toLocalDate());

        return allUsers.stream().map(user -> {
            var userShifts = allShifts.stream().filter(
                    s -> s.getUser().getId().equals(user.getId())).toList();
            var userAbsences = allAbsences.stream().filter(
                    a -> a.getUser().getId().equals(user.getId())).toList();
            return new UserAttendanceAggregate(
                    user.getId(), user.getUsername(), userShifts, userAbsences);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportShiftsToCsv(Long userId, LocalDateTime start, LocalDateTime end) {
        List<WorkShift> shifts = workShiftRepository.findUserShiftsInPeriod(userId, start, end);

        StringBuilder csvBuilder = new StringBuilder(CSV_HEADER);

        for (WorkShift shift : shifts) {
            var dto = workShiftMapper.toResponse(shift);

            // Защита: если смена еще не завершена, пишем текст вместо падения
            String formattedEndTime = shift.getEndTime() != null
                    ? shift.getEndTime().format(TIME_FORMATTER)
                    : "Active";

            csvBuilder.append(dto.getUser().getUsername()).append(",")
                    .append(shift.getId()).append(",")
                    // Дата: 2026-07-21
                    .append(shift.getStartTime().toLocalDate()).append(",")
                    // 🟢 Старт: 09:00
                    .append(shift.getStartTime().format(TIME_FORMATTER)).append(",")
                    // 🟢 Конец: 18:00
                    .append(formattedEndTime).append(",")
                    .append(shift.getBreakDurationMinutes()).append(",")
                    .append(dto.getTotalWorkingTime()).append(",")
                    .append(dto.getClearWorkingTime()).append(",")
                    .append(shift.getRateAtTheTime()).append(",")
                    .append(shift.getProfit()).append(",")
                    .append(shift.getStatusNote() != null
                            ? shift.getStatusNote() : "").append("\n");
        }

        return csvBuilder.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportCompanyShiftsToCsv(LocalDateTime start, LocalDateTime end) {
        StringBuilder csvBuilder = new StringBuilder(CSV_HEADER);

        // 1. Вытягиваем из базы ВСЕ смены компании за период
        // (включая мирные отпускные заглушки робота)
        List<WorkShift> allShifts = workShiftRepository.findAllShiftsInPeriod(start, end);

        // 2. Короткий, понятный и линейный цикл
        for (WorkShift shift : allShifts) {
            appendShiftRow(csvBuilder, shift);
        }
        return csvBuilder.toString();
    }

    private void appendShiftRow(StringBuilder csvBuilder, WorkShift shift) {
        var dto = workShiftMapper.toResponse(shift);

        // Защита: если смена APPROVED_ABSENCE, то старт/конец у нее равен 00:00 и 23:59.
        // Чтобы в Excel это смотрелось красиво,
        // для отпусков мы выведем прочерки "-", а для работы — честное время!
        boolean isAbsence = shift.getStatusNote() != null
                && shift.getStatusNote().startsWith("APPROVED_ABSENCE");

        // Вычисляем текстовые маркеры старта и конца
        String startTimeStr = isAbsence ? "-" : shift.getStartTime().format(TIME_FORMATTER);
        String endTimeStr = isAbsence ? "-" : formatActualEndTime(shift);

        // Нанизываем данные на общую ленту StringBuilder
        csvBuilder.append(dto.getUser().getUsername()).append(",")
                .append(shift.getId()).append(",")
                .append(shift.getStartTime().toLocalDate()).append(",") // Дата дня
                .append(startTimeStr).append(",") // Старт смены (или "-")
                .append(endTimeStr).append(",") // Конец смены (или "-")
                .append(shift.getBreakDurationMinutes()).append(",")
                .append(isAbsence ? "00:00" : dto.getTotalWorkingTime()).append(",")
                .append(isAbsence ? "00:00" : dto.getClearWorkingTime()).append(",")
                .append(shift.getRateAtTheTime()).append(",")
                // Для отпуска тут честно прилетит 0.00 из базы
                .append(shift.getProfit()).append(",")
                .append(shift.getStatusNote() != null ? shift.getStatusNote() : "").append("\n");
    }

    private String formatActualEndTime(WorkShift shift) {
        return shift.getEndTime() != null ? shift.getEndTime().format(TIME_FORMATTER) : "Active";
    }
}
