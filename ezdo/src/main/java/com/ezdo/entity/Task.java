package com.ezdo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    /** Minutes */
    @Column(nullable = false)
    private Integer estimatedDuration;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder.Default
    private Boolean mandatory = false;

    @Builder.Default
    private Integer estimatedPoints = 0;

    @Builder.Default
    private Boolean allowTaskSplitting = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // --- consists_of (N side) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    // --- depends_on (N:N, self-referencing) ---
    // A Task can depend on several prerequisite Tasks.
    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "task_dependency",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "depends_on_task_id")
    )
    private Set<Task> dependsOn = new HashSet<>();

    @Builder.Default
    @ManyToMany(mappedBy = "dependsOn")
    private Set<Task> dependentTasks = new HashSet<>();

    // --- scheduled_as (1:N) — inverse side, owned by Session ---
    @Builder.Default
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Session> sessions = new HashSet<>();

    public boolean isCompleted() {
        return completedAt != null;
    }

    /**
     * Revokes an explicit completion. Called from every path that introduces an
     * unresolved session, so the invariant "a completed task has no SCHEDULED
     * sessions" holds without any operation having to be rejected.
     */
    public void reopen() {
        completedAt = null;
    }
}
