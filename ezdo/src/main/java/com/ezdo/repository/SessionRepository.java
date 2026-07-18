package com.ezdo.repository;

import com.ezdo.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session , UUID> {
    List<Session> findByZoneId (UUID zoneId);

}
