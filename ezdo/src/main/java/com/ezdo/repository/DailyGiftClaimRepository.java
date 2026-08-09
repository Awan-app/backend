package com.ezdo.repository;

import com.ezdo.entity.DailyGiftClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyGiftClaimRepository extends JpaRepository<DailyGiftClaim, UUID> {

    boolean existsByUserIdAndClaimDate(UUID userId, LocalDate claimDate);

    Optional<DailyGiftClaim> findTopByUserIdOrderByClaimDateDesc(UUID userId);
}
