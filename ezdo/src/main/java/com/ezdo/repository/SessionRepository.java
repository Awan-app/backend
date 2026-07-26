package com.ezdo.repository;

import com.ezdo.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session , UUID> {
    @Query("""
        SELECT s FROM Session s
        JOIN s.task t
        JOIN t.goal g
        WHERE t.id = :taskId AND g.user.id = :userId
    """)
    List<Session> findByTaskIdAndUserId(@Param("taskId") UUID taskId, @Param("userId") UUID userId);

    @Query("""
        SELECT s FROM Session s
        JOIN s.task t
        JOIN t.goal g
        WHERE s.id = :id AND g.user.id = :userId
    """)
    Optional<Session> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
        SELECT s FROM Session s
        JOIN s.zone z
        LEFT JOIN z.template t
        LEFT JOIN z.templateOverride o
        WHERE z.id = :zoneId AND (t.user.id = :userId OR o.user.id = :userId)
    """)
    List<Session> findByZoneIdAndUserId(@Param("zoneId") UUID zoneId, @Param("userId") UUID userId);

    @Query("""
        SELECT s FROM Session s
        JOIN s.task t
        JOIN t.goal g
        WHERE g.user.id = :userId
          AND s.start >= :startDate
          AND s.start < :endDate
        ORDER BY s.start ASC
    """)
    List<Session> findByUserIdAndDateRange(@Param("userId") UUID userId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT s FROM Session s
        JOIN s.task t
        JOIN t.goal g
        WHERE g.user.id = :userId 
        AND s.start >= :fromTime AND s.end <= :toTime
    """)
    List<Session> findByUserIdAndTimeRange(
        @Param("userId") UUID userId,
        @Param("fromTime") LocalDateTime fromTime,
        @Param("toTime") LocalDateTime toTime
    );

    @Query("SELECT s FROM Session s WHERE s.task.goal.user.id = :userId " +
            "AND s.start >= :dayStart AND s.start < :dayEnd")
    List<Session> findByUserIdAndDate(@Param("userId") UUID userId,
                                      @Param("dayStart") LocalDateTime dayStart,
                                      @Param("dayEnd") LocalDateTime dayEnd);
}
