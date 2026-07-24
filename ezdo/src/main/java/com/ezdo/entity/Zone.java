package com.ezdo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A time block within a Template OR a TemplateOverride (never both — the diagram
 * shows "consists_of" 1:N from each parent into the same Zone entity), which in
 * turn is realized by one or more Sessions (belongs_to, 1:N).
 */
@Entity
@Table(name = "zones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // --- consists_of (N side) — nullable: set only when the Zone belongs to a Template
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

    // --- consists_of (N side) — nullable: set only when the Zone belongs to a TemplateOverride
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_override_id")
    private TemplateOverride templateOverride;

    // --- belongs_to (1:N) ---
    @Builder.Default
    @OneToMany(mappedBy = "zone")
    private List<Session> sessions = new ArrayList<>();
}
