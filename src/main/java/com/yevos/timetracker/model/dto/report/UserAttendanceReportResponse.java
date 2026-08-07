package com.yevos.timetracker.model.dto.report;

import com.yevos.timetracker.model.dto.response.AbsenceResponse;
import com.yevos.timetracker.model.dto.response.WorkShiftResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAttendanceReportResponse {

    private Long userId;
    private String username;
    private List<WorkShiftResponse> shifts; // Все факты реальной работы (DTO)
    private List<AbsenceResponse> absences;
}
