package com.yevos.timetracker.repository;

import com.yevos.timetracker.model.entity.AbsenceRecord;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AbsenceRepository extends JpaRepository<AbsenceRecord, Long> {

    @Query("SELECT a FROM AbsenceRecord a WHERE a.user.id = :userId "
            + "AND a.startDate <= :end AND a.endDate >= :start")
    List<AbsenceRecord> findUserAbsencesInPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("SELECT a FROM AbsenceRecord a WHERE a.startDate <= :end AND a.endDate >= :start")
    List<AbsenceRecord> findAllAbsencesInPeriod(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
