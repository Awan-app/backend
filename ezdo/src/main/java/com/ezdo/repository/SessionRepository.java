package com.ezdo.repository;

import com.ezdo.entity.Session;
import com.ezdo.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
          AND (:status IS NULL OR s.status = :status)
    """)
    List<Session> findByTaskIdAndUserId(@Param("taskId") UUID taskId, @Param("userId") UUID userId,
                                        @Param("status") SessionStatus status);

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
          AND (:status IS NULL OR s.status = :status)
    """)
    List<Session> findByZoneIdAndUserId(@Param("zoneId") UUID zoneId, @Param("userId") UUID userId,
                                        @Param("status") SessionStatus status);

    @Query("""
        SELECT s FROM Session s
        JOIN s.task t
        JOIN t.goal g
        WHERE g.user.id = :userId
          AND s.start >= :startDate
          AND s.start < :endDate
          AND (:status IS NULL OR s.status = :status)
        ORDER BY s.start ASC
    """)
    List<Session> findByUserIdAndDateRange(@Param("userId") UUID userId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("status") SessionStatus status);

    /**
     * Same range as {@link #findByUserIdAndDateRange}, but the task is fetch-joined.
     * The plain query joins {@code s.task} only to filter by owner, which leaves the
     * association a lazy proxy — fine for reading its id, but any caller that needs
     * the task's title or flags (the AI schedule context does) would N+1 or blow up
     * outside a transaction.
     */
    @Query("""
        SELECT s FROM Session s
        JOIN FETCH s.task t
        LEFT JOIN FETCH t.category
        JOIN t.goal g
        WHERE g.user.id = :userId
          AND s.start >= :startDate
          AND s.start < :endDate
        ORDER BY s.start ASC
    """)
    List<Session> findByUserIdAndDateRangeWithTask(@Param("userId") UUID userId,
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

    /**
     * Still-{@code SCHEDULED} sessions whose window has already ended (missed),
     * within a required {@code [startDate, endDate)} range.
     */
    @Query("""
        SELECT s FROM Session s
        JOIN s.task t
        JOIN t.goal g
        WHERE g.user.id = :userId
          AND s.status = SCHEDULED
          AND s.end < :now
          AND s.start >= :startDate
          AND s.start < :endDate
        ORDER BY s.start ASC
    """)
    List<Session> findMissed(@Param("userId") UUID userId,
                             @Param("now") LocalDateTime now,
                             @Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);
    @Modifying
    @Query("UPDATE Session s SET s.zone = null WHERE s.zone.id IN :zoneIds")
    void nullifyZoneId(@Param("zoneIds") List<UUID> zoneIds);
}
