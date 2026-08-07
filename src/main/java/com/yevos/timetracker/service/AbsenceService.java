package com.yevos.timetracker.service;

import com.yevos.timetracker.model.dto.request.AbsenceRequest;
import com.yevos.timetracker.model.entity.AbsenceRecord;
import java.time.LocalDate;
import java.util.List;

public interface AbsenceService {

    AbsenceRecord createAbsence(String username, AbsenceRequest request);

    List<AbsenceRecord> getUserAbsences(Long userId, LocalDate start, LocalDate end);
}
