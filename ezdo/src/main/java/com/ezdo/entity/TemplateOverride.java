package com.ezdo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A one-off override of the recurring Template for a specific date,
 * created by a User, consisting of its own Zones.
 */
@Entity
@Table(name = "template_overrides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private LocalDate dateOfDay; // FEAT: AI can suggest making it as default...

    // --- creates (N side) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // --- consists_of (1:N) ---
    @Builder.Default
    @OneToMany(mappedBy = "templateOverride", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Zone> zones = new ArrayList<>();
}
