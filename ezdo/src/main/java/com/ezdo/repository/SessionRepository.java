package com.ezdo.repository;

import com.ezdo.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session , UUID> {
    List<Session> findByZoneId (UUID zoneId);

    @Query("""
        SELECT s FROM Session s 
        JOIN s.zone z 
        LEFT JOIN z.template t 
        LEFT JOIN z.templateOverride o 
        WHERE s.id = :id AND (t.user.id = :userId OR o.user.id = :userId)
    """)
    Optional<Session> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

}
