package com.yevos.timetracker.controller;

import com.yevos.timetracker.mapper.AbsenceMapper;
import com.yevos.timetracker.mapper.WorkShiftMapper;
import com.yevos.timetracker.model.dto.report.UserAttendanceReportResponse;
import com.yevos.timetracker.model.dto.response.AbsenceResponse;
import com.yevos.timetracker.model.dto.response.WorkShiftResponse;
import com.yevos.timetracker.security.service.UserDetailsImpl;
import com.yevos.timetracker.service.AttendanceReportService;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class AttendanceReportControllerV1 {

    private final AttendanceReportService reportService;
    private final WorkShiftMapper workShiftMapper;
    private final AbsenceMapper absenceMapper;

    public AttendanceReportControllerV1(AttendanceReportService reportService,
                                        WorkShiftMapper workShiftMapper,
                                        AbsenceMapper absenceMapper) {
        this.reportService = reportService;
        this.workShiftMapper = workShiftMapper;
        this.absenceMapper = absenceMapper;
    }

    @GetMapping("/admin/company-attendance")
    @Operation(summary = "Get consolidated report of shifts "
            + "and absences for all employees within a date range (YYYY-MM-DD)")
    public ResponseEntity<List<UserAttendanceReportResponse>> getCompanyAttendanceReport(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        var aggregates = reportService.getCompanyAttendanceData(startDateTime, endDateTime);

        var response = aggregates.stream().map(agg -> {
            List<WorkShiftResponse> shifts = agg.getShifts().stream()
                    .map(workShiftMapper::toResponse).toList();
            List<AbsenceResponse> absences = agg.getAbsences().stream()
                    .map(absenceMapper::toResponse).toList();
            return new UserAttendanceReportResponse(
                    agg.getUserId(), agg.getUsername(), shifts, absences);
        }).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/company-export/csv")
    @Operation(summary = "Export and download consolidated financial "
            + "and attendance report for ALL employees as a CSV file (YYYY-MM-DD)")
    public ResponseEntity<byte[]> exportCompanyReportToCsv(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        // 1. Вычисляем точные границы времени для всего периода
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // 2. Вызываем наш отрефакторенный, чистый метод сервиса аналитики
        String csvData = reportService.exportCompanyShiftsToCsv(startDateTime, endDateTime);

        // 3. Упаковываем с помощью вашего приватного BOM-метода (защита кириллицы в Excel)
        byte[] fileBytes = addBomToCsvBytes(csvData);

        // 4. Формируем красивое говорящее имя файла, например:
        // company_report_2026-07-01_2026-07-31.csv
        String fileName = String.format("company_report_%s_%s.csv", startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(fileBytes);
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export and download user monthly financial report as a CSV file")
    public ResponseEntity<byte[]> exportMonthlyReportToCsv(
            @AuthenticationPrincipal UserDetailsImpl userPrincipal,
            @RequestParam("year") int year,
            @RequestParam("month") int month) {

        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        int lastDay = LocalDate.of(year, month, 1).lengthOfMonth();
        LocalDateTime end = LocalDateTime.of(year, month, lastDay, 23, 59, 59);

        // Получаем готовую текстовую CSV-таблицу из сервиса
        String csvData = reportService.exportShiftsToCsv(userPrincipal.getId(), start, end);

        byte[] fileBytes = addBomToCsvBytes(csvData); // Ваш приватный метод с BOM
        String fileName = String.format("report_%d_%d.csv", month, year);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="
                        + fileName)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(fileBytes);
    }

    private byte[] addBomToCsvBytes(String csvData) {

        byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] textBytes = csvData.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] resultBytes = new byte[bom.length + textBytes.length];
        System.arraycopy(bom, 0, resultBytes, 0, bom.length);
        System.arraycopy(textBytes, 0, resultBytes, bom.length, textBytes.length);
        return resultBytes;
    }
}
