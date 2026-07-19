package com.ezdo.repository;

import com.ezdo.entity.Template;
import com.ezdo.entity.TemplateOverride;
import com.ezdo.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZoneRepository extends JpaRepository<Zone , UUID> {

    List<Zone> findByTemplateId(UUID templateId);
    List<Zone> findByTemplateOverrideId(UUID templateOverrideId);

    @Query("""
        SELECT z FROM Zone z 
        LEFT JOIN z.template t 
        LEFT JOIN z.templateOverride o 
        WHERE z.id = :id AND (t.user.id = :userId OR o.user.id = :userId)
    """)
    Optional<Zone> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

}
