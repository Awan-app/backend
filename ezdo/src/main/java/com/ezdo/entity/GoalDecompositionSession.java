package com.ezdo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "goal_decomposition_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalDecompositionSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DecompositionStatus status = DecompositionStatus.IN_PROGRESS;

    @Column(columnDefinition = "JSON")
    private String messages;

    @Column(name = "proposed_goal", columnDefinition = "JSON")
    private String proposedGoal;

    @Column(name = "confirmed_goal_id")
    private UUID confirmedGoalId;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;
}
