package com.yevos.timetracker.controller;

import com.yevos.timetracker.mapper.AbsenceMapper;
import com.yevos.timetracker.model.dto.request.AbsenceRequest;
import com.yevos.timetracker.model.dto.response.AbsenceResponse;
import com.yevos.timetracker.model.entity.AbsenceRecord;
import com.yevos.timetracker.security.service.UserDetailsImpl;
import com.yevos.timetracker.service.AbsenceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/absences")
@Validated
public class AbsenceControllerV1 {

    private final AbsenceService absenceService;
    private final AbsenceMapper absenceMapper;

    public AbsenceControllerV1(AbsenceService absenceService, AbsenceMapper absenceMapper) {
        this.absenceService = absenceService;
        this.absenceMapper = absenceMapper;
    }

    @PostMapping("/admin")
    @Operation(summary = "Register a new employee absence record (vacation/sick leave) "
            + "by Administrator with overlap protection")
    public ResponseEntity<AbsenceResponse> createAbsence(
            @RequestParam("username") String username,
            @RequestBody @Valid AbsenceRequest request) {

        AbsenceRecord rawAbsence = absenceService.createAbsence(username, request);
        AbsenceResponse response = absenceMapper.toResponse(rawAbsence);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<AbsenceResponse>> getAbsenceHistory(
            @AuthenticationPrincipal UserDetailsImpl userPrincipal,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        List<AbsenceRecord> records = absenceService.getUserAbsences(userPrincipal.getId(),
                start, end);
        List<AbsenceResponse> response = records.stream()
                .map(absenceMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }
}
