package com.ezdo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

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

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "streak_change_dates",
            joinColumns = @JoinColumn(name = "streak_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_streak_change_date",
                    columnNames = {"streak_id", "change_date"})
    )
    @Column(name = "change_date", nullable = false)
    private Set<LocalDate> streakChangeDates = new LinkedHashSet<>();

    public int effectiveStreak(LocalDate today) {
        if (lastActivityDate == null || lastActivityDate.isBefore(today.minusDays(1))) {
            return 0;
        }
        return currentStreak;
    }
}
