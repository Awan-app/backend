package com.ezdo.repository;

import com.ezdo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.profilePictureUrl = :url WHERE u.id = :userId")
    void updateProfilePictureUrl(@Param("userId") UUID userId, @Param("url") String url);

    @Query("SELECT u FROM User u WHERE u.preferences.wakeupTime IS NOT NULL " +
            "AND u.preferences.dailySummaryEnabled = true")
    List<User> findAllEligibleForDailySummary();
}
