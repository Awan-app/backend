package com.ezdo.repository;

import com.ezdo.entity.TemplateOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateOverrideRepository extends JpaRepository<TemplateOverride , UUID> {

    List<TemplateOverride> findByUserId(UUID userId);
    Optional<TemplateOverride> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT o FROM TemplateOverride o LEFT JOIN FETCH o.zones WHERE o.user.id = :userId AND o.dateOfDay = :dateOfDay")
    Optional<TemplateOverride> findByUserIdAndDateOfDayWithZones(@Param("userId") UUID userId, @Param("dateOfDay") LocalDate dateOfDay);
}
