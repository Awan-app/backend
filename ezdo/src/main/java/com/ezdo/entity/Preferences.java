package com.ezdo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Preferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private String timezone;

    /** Minutes */
    private Integer preferredSessionDuration;

    /** Minutes */
    private Integer bufferBetweenSessions;

    @Column(name = "wakeup_time")
    private LocalTime wakeupTime;

    @Column(name = "sleep_time")
    private LocalTime sleepTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "scheduling_type")
    private SchedulingType schedulingType;

    @Column(name = "daily_summary_enabled", nullable = false)
    @Builder.Default
    private Boolean dailySummaryEnabled = true;
}
