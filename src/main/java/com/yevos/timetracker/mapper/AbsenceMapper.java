package com.yevos.timetracker.mapper;

import com.yevos.timetracker.model.dto.response.AbsenceResponse;
import com.yevos.timetracker.model.entity.AbsenceRecord;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class AbsenceMapper {

    public AbsenceResponse toResponse(AbsenceRecord absenceRecord) {

        if (absenceRecord == null) {
            return null;
        }
        AbsenceResponse response = new AbsenceResponse();
        response.setId(absenceRecord.getId());
        response.setStartDate(absenceRecord.getStartDate());
        response.setEndDate(absenceRecord.getEndDate());
        response.setAbsenceType(absenceRecord.getAbsenceType());
        response.setReason(absenceRecord.getReason());
        response.setTotalDays(calculateTotalDays(absenceRecord.getStartDate(),
                absenceRecord.getEndDate()));

        return response;
    }

    private int calculateTotalDays(LocalDate start, LocalDate end) {

        return (int) (end.toEpochDay() - start.toEpochDay()) + 1;
    }
}

