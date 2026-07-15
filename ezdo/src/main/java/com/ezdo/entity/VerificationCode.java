package com.ezdo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "verification_codes",
        indexes = @Index(name = "idx_verification_codes_email", columnList = "email")
)
public class VerificationCode {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private int attempts = 0;

    private boolean consumed = false;

    private boolean locked = false;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}