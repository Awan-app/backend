package com.ezdo.repository;

import com.ezdo.entity.Template;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public interface TemplateRepository extends JpaRepository<Template, UUID> {
    List<Template> findByUserId(UUID userId);
    Optional<Template> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT t FROM Template t JOIN t.daysOfWeek d WHERE t.user.id = :userId AND d IN :days")
    List<Template> findTemplatesWithConflictingDays(UUID userId, Set<DayOfWeek> days);

    @Query("""
        SELECT COUNT(t) > 0
        FROM Template t
        JOIN t.daysOfWeek d
        WHERE t.user.id = :userId
        AND d IN :days
    """)
    boolean existsByUserIdAndDaysOfWeekIn(UUID userId , Set<DayOfWeek> days);


    @Query("""
SELECT COUNT(t) > 0
FROM Template t
JOIN t.daysOfWeek d
WHERE t.user.id = :userId
AND t.id <> :templateId
AND d IN :daysOfWeek
""")
    boolean existsByUserIdAndIdNotAndDaysOfWeekIn(
            UUID userId,
            UUID templateId,
            Set<DayOfWeek> daysOfWeek);

    @Query("""
    SELECT DISTINCT t
    FROM Template t
    JOIN FETCH t.zones
    JOIN t.daysOfWeek d
    WHERE t.user.id = :userId
      AND d = :dayOfWeek
""")
    Optional<Template> findByUserIdAndDayOfWeekWithZones(@Param("userId") UUID userId, @Param("dayOfWeek") DayOfWeek dayOfWeek);

    /**
     * Every template a user has, with its weekdays, zones and zone categories all
     * fetched. Callers resolving a date range index these by day-of-week in memory
     * rather than issuing a query per day.
     */
    @Query("""
        SELECT DISTINCT t FROM Template t
        LEFT JOIN FETCH t.zones z
        LEFT JOIN FETCH z.category
        LEFT JOIN FETCH t.daysOfWeek
        WHERE t.user.id = :userId
    """)
    List<Template> findByUserIdWithZones(@Param("userId") UUID userId);

}
