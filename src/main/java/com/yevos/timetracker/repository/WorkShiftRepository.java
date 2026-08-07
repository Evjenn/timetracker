package com.yevos.timetracker.repository;

import com.yevos.timetracker.model.entity.WorkShift;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {

    Optional<WorkShift> findByUserIdAndEndTimeIsNull(Long userId);

    @Query("SELECT w FROM WorkShift w WHERE w.user.id = :userId "
            + "AND w.startTime >= :start AND w.startTime <= :end")
    List<WorkShift> findUserShiftsInPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Проверяет наличие реальных рабочих смен пользователя за выбранный период.
     * Технические штрафные заглушки робота (FORGOTTEN_START_ALERT) игнорируются,
     * чтобы админ мог беспрепятственно вносить больничные и отпуска задним числом.
     */
    @Query("SELECT COUNT(w) > 0 FROM WorkShift w WHERE w.user.id = :userId "
            + "AND w.startTime >= :start AND w.startTime <= :end "
            + "AND (w.statusNote IS NULL OR w.statusNote != 'FORGOTTEN_START_ALERT')")
    boolean hasShiftsInPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT w FROM WorkShift w WHERE w.user.username = :username "
            + "AND w.startTime >= :start AND w.startTime <= :end")
    Optional<WorkShift> findByUsernameAndPeriod(
            @Param("username") String username,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Метод для админского сводного отчета по всей компании
    @Query("SELECT w FROM WorkShift w WHERE w.startTime >= :start AND w.startTime <= :end")
    List<WorkShift> findAllShiftsInPeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<WorkShift> findByStatusNoteStartingWith(String prefix);
}
