package com.ezdo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A long-lived API key that lets MCP clients (Claude, Cursor, VS Code, etc.)
 * authenticate as a specific EZDO user without short-lived JWTs.
 *
 * <p>The key is a random UUID string generated once and shown to the user.
 * It is stored as-is (not hashed) for simplicity — if you later want to
 * hash it, store a prefix for lookup and hash the rest.
 */
@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The actual key string the user pastes into their MCP client config. */
    @Column(name = "key_value", nullable = false, unique = true, length = 64)
    private String keyValue;

    /** A human-readable label so the user can manage multiple keys. */
    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
