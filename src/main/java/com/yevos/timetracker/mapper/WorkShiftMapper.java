package com.yevos.timetracker.mapper;

import com.yevos.timetracker.model.dto.response.UserShortResponse;
import com.yevos.timetracker.model.dto.response.WorkShiftResponse;
import com.yevos.timetracker.model.entity.WorkShift;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class WorkShiftMapper {

    public WorkShiftResponse toResponse(WorkShift shift) {

        if (shift == null) {
            return null;
        }
        WorkShiftResponse response = new WorkShiftResponse();
        response.setId(shift.getId());
        response.setStartTime(shift.getStartTime());
        response.setEndTime(shift.getEndTime());
        response.setBreakDurationMinutes(shift.getBreakDurationMinutes());
        response.setRateAtTheTime(shift.getRateAtTheTime());
        response.setProfit(shift.getProfit());
        response.setTotalWorkingTime(calculateTotalWorkingTime(shift));
        response.setClearWorkingTime(calculateClearWorkingTime(shift));
        response.setCompleted(shift.getEndTime() != null);
        response.setStatusNote(shift.getStatusNote());

        if (shift.getUser() != null) {
            UserShortResponse userResponse = new UserShortResponse();
            userResponse.setId(shift.getUser().getId());
            userResponse.setUsername(shift.getUser().getUsername());
            response.setUser(userResponse);
        }

        return response;
    }

    private String calculateTotalWorkingTime(WorkShift shift) {

        LocalDateTime endPoint = shift.getEndTime() != null
                ? shift.getEndTime() : LocalDateTime.now();
        long totalMinutes = Duration.between(shift.getStartTime(), endPoint).toMinutes();
        return formatMinutes(totalMinutes);
    }

    private String calculateClearWorkingTime(WorkShift shift) {

        LocalDateTime endPoint = shift.getEndTime() != null
                ? shift.getEndTime() : LocalDateTime.now();
        long totalMinutes = Duration.between(shift.getStartTime(), endPoint).toMinutes();
        int breakMins = shift.getBreakDurationMinutes() != null
                ? shift.getBreakDurationMinutes() : 0;

        long clearMinutes = totalMinutes - breakMins;
        return formatMinutes(clearMinutes < 0 ? 0 : clearMinutes);
    }

    private String formatMinutes(long totalMinutes) {

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
