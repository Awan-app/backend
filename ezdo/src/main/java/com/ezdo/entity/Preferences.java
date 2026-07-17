package com.ezdo.entity;

import jakarta.persistence.*;
import lombok.*;

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
}
