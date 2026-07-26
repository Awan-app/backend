package com.ezdo.repository;

import com.ezdo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.preferences.wakeupTime IS NOT NULL " +
            "AND u.preferences.dailySummaryEnabled = true")
    List<User> findAllEligibleForDailySummary();
}
