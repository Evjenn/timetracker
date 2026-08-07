package com.yevos.timetracker.model.dto.request;

import com.yevos.timetracker.model.entity.AbsenceType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AbsenceRequest {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Absence type is required")
    private AbsenceType absenceType;

    private String reason;
}
