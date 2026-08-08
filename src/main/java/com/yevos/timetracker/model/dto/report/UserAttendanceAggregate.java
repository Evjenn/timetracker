package com.yevos.timetracker.model.dto.report;

import com.yevos.timetracker.model.entity.AbsenceRecord;
import com.yevos.timetracker.model.entity.WorkShift;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAttendanceAggregate {

    private Long userId;
    private String username;
    private List<WorkShift> shifts;
    private List<AbsenceRecord> absences;
}
