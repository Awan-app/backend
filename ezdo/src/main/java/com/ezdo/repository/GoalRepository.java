package com.ezdo.repository;

import com.ezdo.entity.Goal;
import com.ezdo.entity.GoalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    Optional<Goal> findByIdAndUserId(UUID goalId, UUID userId);

    Optional<Goal> findByUserIdAndInboxTrue(UUID userId);

    Page<Goal> findByUserId(UUID userId, Pageable pageable);

    Page<Goal> findByUserIdAndStatus(UUID userId, GoalStatus status, Pageable pageable);

    Page<Goal> findByUserIdAndInboxFalse(UUID userId, Pageable pageable);

    Page<Goal> findByUserIdAndStatusAndInboxFalse(UUID userId, GoalStatus status, Pageable pageable);
}
