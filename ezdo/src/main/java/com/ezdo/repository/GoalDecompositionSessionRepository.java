package com.ezdo.repository;

import com.ezdo.entity.GoalDecompositionSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoalDecompositionSessionRepository extends JpaRepository<GoalDecompositionSession, UUID> {
    Optional<GoalDecompositionSession> findByIdAndUserId(UUID id, UUID userId);
}
