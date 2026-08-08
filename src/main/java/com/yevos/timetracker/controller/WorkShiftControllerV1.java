package com.yevos.timetracker.controller;

import com.yevos.timetracker.mapper.WorkShiftMapper;
import com.yevos.timetracker.model.dto.response.WorkShiftResponse;
import com.yevos.timetracker.model.entity.WorkShift;
import com.yevos.timetracker.security.service.UserDetailsImpl;
import com.yevos.timetracker.service.WorkShiftService;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shifts")
@Validated
public class WorkShiftControllerV1 {

    private final WorkShiftService workShiftService;
    private final WorkShiftMapper workShiftMapper;

    public WorkShiftControllerV1(WorkShiftService workShiftService,
                                 WorkShiftMapper workShiftMapper) {
        this.workShiftService = workShiftService;
        this.workShiftMapper = workShiftMapper;
    }

    @PostMapping("/start")
    public ResponseEntity<String> startShift(
            @AuthenticationPrincipal UserDetailsImpl userPrincipal) {
        workShiftService.startShift(userPrincipal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body("Work shift started successfully");
    }

    @PostMapping("/end")
    public ResponseEntity<String> endShift(@AuthenticationPrincipal UserDetailsImpl userPrincipal) {
        workShiftService.endShift(userPrincipal.getId());
        return ResponseEntity.ok("Work shift closed successfully");
    }

    @PostMapping("/break/start")
    public ResponseEntity<String> startBreak(@AuthenticationPrincipal
                                                 UserDetailsImpl userPrincipal) {
        workShiftService.startBreak(userPrincipal.getId());
        return ResponseEntity.ok("Break started successfully");
    }

    @PostMapping("/break/end")
    public ResponseEntity<String> endBreak(@AuthenticationPrincipal UserDetailsImpl userPrincipal) {
        workShiftService.endBreak(userPrincipal.getId());
        return ResponseEntity.ok("Break ended successfully. Total duration updated.");
    }

    @GetMapping("/history")
    @Operation(summary = "Get user work shift history filtered by date range in format YYYY-MM-DD")
    public ResponseEntity<List<WorkShiftResponse>> getShiftsHistory(
            @AuthenticationPrincipal UserDetailsImpl userPrincipal,
            @RequestParam("startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<WorkShift> shifts = workShiftService.getShiftsInPeriod(userPrincipal.getId(),
                startDateTime, endDateTime);
        List<WorkShiftResponse> response = shifts.stream()
                .map(workShiftMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/admin/manage-shift/{shiftId}")
    @Operation(
            summary = "Manually update shift fields by Admin with automatic profit recalculation"
    )
    public ResponseEntity<String> adminModifyShift(
            @PathVariable("shiftId")
            Long shiftId,
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime,
            @RequestParam("breakMinutes") int breakMinutes,
            @RequestParam("note") String note) {

        workShiftService.adminUpdateShift(shiftId, startTime, endTime, breakMinutes, note);
        return ResponseEntity.ok(
                "Shift ID " + shiftId + " successfully updated by Admin. Note: " + note);
    }

    @GetMapping("/admin/search-shift")
    @Operation(summary = "Find a specific work shift by employee username and date (YYYY-MM-DD)")
    public ResponseEntity<WorkShiftResponse> findShiftByUsernameAndDate(
            @RequestParam("username") String username,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(23, 59, 59);

        WorkShift shift = workShiftService.getShiftByUsernameAndDate(username,
                startDateTime, endDateTime);

        return ResponseEntity.ok(workShiftMapper.toResponse(shift));
    }

    @GetMapping("/admin/alerts")
    @Operation(summary = "Get all unresolved attendance alerts for administrator audit")
    public ResponseEntity<List<WorkShiftResponse>> getAdminAlerts() {

        List<WorkShift> alerts = workShiftService.getAdminAlerts();
        List<WorkShiftResponse> response = alerts.stream()
                .map(workShiftMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

}
