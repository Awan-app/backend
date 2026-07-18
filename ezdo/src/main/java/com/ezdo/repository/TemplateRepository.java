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
    WHERE t.id = :templateId
      AND d = :dayOfWeek
""")
    Optional<Template> findByWeeklyTemplateIdAndDayOfWeekWithZones(@Param("templateId") UUID templateId, @Param("dayOfWeek") DayOfWeek dayOfWeek);

}
