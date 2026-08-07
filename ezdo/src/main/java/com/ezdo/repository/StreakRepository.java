package com.ezdo.repository;

import com.ezdo.entity.Streak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StreakRepository extends JpaRepository<Streak, UUID> {

    @Query("SELECT d FROM Streak s JOIN s.streakChangeDates d " +
            "WHERE s.id = :userId AND d BETWEEN :startDate AND :endDate ORDER BY d")
    List<LocalDate> findActivityDates(@Param("userId") UUID userId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);
}
