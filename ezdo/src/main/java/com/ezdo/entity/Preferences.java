package com.ezdo.entity;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "preferences")
public class Preferences {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private LocalTime workingHoursStart;

    @Column(nullable = false)
    private LocalTime workingHoursEnd;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "preferences_work_days", joinColumns = @JoinColumn(name = "preferences_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private Set<DayOfWeek> workDays = new HashSet<>();

    @Column(nullable = false)
    private Duration preferredSessionDuration;

    @Column(nullable = false)
    private Duration maxDailyWorkload;

    @Column(nullable = false)
    private Duration bufferBetweenSessions;

    @Column(nullable = false)
    private boolean allowTaskSplitting = true;
}
