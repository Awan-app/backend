package com.ezdo.repository;

import com.ezdo.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

    Optional<VerificationCode> findFirstByEmailAndConsumedFalseAndLockedFalseOrderByCreatedAtDesc(String email);

    long countByEmailAndCreatedAtAfter(String email, Instant since);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE VerificationCode v SET v.consumed = true WHERE v.email = :email AND v.consumed = false")
    void invalidateUnconsumedByEmail(String email);

    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
