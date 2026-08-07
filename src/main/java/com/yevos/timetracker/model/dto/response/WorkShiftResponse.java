package com.yevos.timetracker.model.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkShiftResponse {

    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer breakDurationMinutes;
    private BigDecimal rateAtTheTime;
    private BigDecimal profit;
    private String totalWorkingTime;
    private String clearWorkingTime;
    private boolean completed;
    private UserShortResponse user;
    private String statusNote;
}
