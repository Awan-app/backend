package com.ezdo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Consecutive days on which the user performed a qualifying activity.
 *
 * <p>Deliberately knows nothing about what counts as qualifying — it only stores
 * activity dates, so new triggers can be added without touching this model.
 *
 * <p>There is no reset job. A lapsed streak is derived on read via
 * {@link #effectiveStreak(LocalDate)}; the stored value self-heals on the next
 * recorded activity, which restarts the count at 1.
 */
@Entity
@Table(name = "streaks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Streak {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Version
    private Long version;

    @Builder.Default
    @Column(name = "current_streak")
    private int currentStreak = 0;

    @Builder.Default
    @Column(name = "max_streak")
    private int maxStreak = 0;

    /** In the user's own timezone, not UTC. */
    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    /**
     * The streak as it should be shown right now: the stored count while the last
     * activity was today or yesterday, otherwise 0 because a full day was missed.
     */
    public int effectiveStreak(LocalDate today) {
        if (lastActivityDate == null || lastActivityDate.isBefore(today.minusDays(1))) {
            return 0;
        }
        return currentStreak;
    }
}
