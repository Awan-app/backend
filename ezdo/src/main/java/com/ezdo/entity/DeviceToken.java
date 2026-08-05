package com.ezdo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "device_tokens",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "device_id"})
    },
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_fcm_token", columnList = "fcm_token")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Unique identifier for the device (e.g., device UUID, installation ID)
     * Helps identify and update tokens for the same device
     */
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    /**
     * Firebase Cloud Messaging token for this device
     */
    @Column(name = "fcm_token", nullable = false, unique = true)
    private String fcmToken;

    /**
     * Device type/platform (optional, for analytics)
     */
    @Column(name = "device_type")
    private String deviceType; // e.g., "ANDROID", "IOS", "WEB"

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
