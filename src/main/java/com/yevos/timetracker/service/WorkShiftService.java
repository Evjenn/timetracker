package com.yevos.timetracker.service;

import com.yevos.timetracker.model.entity.WorkShift;
import java.time.LocalDateTime;
import java.util.List;

public interface WorkShiftService {

    WorkShift startShift(Long userId);

    WorkShift endShift(Long userId);

    List<WorkShift> getShiftsInPeriod(Long userId, LocalDateTime start, LocalDateTime end);

    void startBreak(Long userId);

    void endBreak(Long userId);

    void adminUpdateShift(Long shiftId, LocalDateTime start,
                                 LocalDateTime end, int breaks, String note);

    WorkShift getShiftByUsernameAndDate(
            String username, LocalDateTime start, LocalDateTime end);

    List<WorkShift> getAdminAlerts();

}
