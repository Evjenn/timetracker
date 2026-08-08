package com.yevos.timetracker.model.dto.response;

import com.yevos.timetracker.model.entity.AbsenceType;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AbsenceResponse {

    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private AbsenceType absenceType;
    private String reason;
    private int totalDays;
}
