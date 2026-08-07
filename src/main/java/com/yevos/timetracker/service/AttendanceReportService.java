package com.yevos.timetracker.service;

import com.yevos.timetracker.model.dto.report.UserAttendanceAggregate;
import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceReportService {

    List<UserAttendanceAggregate> getCompanyAttendanceData(LocalDateTime start, LocalDateTime end);

    String exportShiftsToCsv(Long userId, LocalDateTime start, LocalDateTime end);

    String exportCompanyShiftsToCsv(LocalDateTime start, LocalDateTime end);
}
