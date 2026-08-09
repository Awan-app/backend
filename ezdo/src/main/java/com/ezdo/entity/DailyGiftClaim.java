package com.ezdo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_gift_claims",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_daily_gift_user_date",
        columnNames = {"user_id", "claim_date"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyGiftClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "claim_date", nullable = false)
    private LocalDate claimDate;

    @Column(name = "segment_id", nullable = false)
    private String segmentId;

    @Column(name = "coins_awarded", nullable = false)
    private int coinsAwarded;

    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;
}
