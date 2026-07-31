package com.ezdo.controller;

import com.ezdo.entity.ApiKey;
import com.ezdo.entity.User;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.repository.ApiKeyRepository;
import com.ezdo.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Manages long-lived API keys for MCP client authentication.
 *
 * <p>Users can generate, list, and revoke API keys from the mobile app.
 * Each key is pasted once into the MCP client config (Claude Desktop,
 * Cursor, VS Code, etc.) and works until revoked.
 */
@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    // ── DTOs (inline records) ────────────────────────────────────────────────

    public record CreateApiKeyRequest(
        @NotBlank @Size(max = 100) String name
    ) {}

    public record ApiKeyResponse(
        UUID id,
        String name,
        String keyValue,   // only shown on creation
        Instant createdAt
    ) {}

    public record ApiKeySummaryResponse(
        UUID id,
        String name,
        String keyPrefix,  // first 8 chars + "..." for display
        Instant createdAt
    ) {}

    // ── endpoints ────────────────────────────────────────────────────────────

    /**
     * Generates a new API key. The full key is returned ONCE — the user must
     * copy it immediately. It cannot be retrieved again after this call.
     */
    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateApiKeyRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String keyValue = "ezdo_" + UUID.randomUUID().toString().replace("-", "")
                                 + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        ApiKey apiKey = ApiKey.builder()
                .name(request.name())
                .keyValue(keyValue)
                .user(user)
                .active(true)
                .createdAt(Instant.now())
                .build();

        apiKeyRepository.save(apiKey);

        return ResponseEntity.ok(new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                keyValue,           // shown only this once
                apiKey.getCreatedAt()
        ));
    }

    /**
     * Lists all active API keys for the current user.
     * The full key value is NOT returned — only a prefix for identification.
     */
    @GetMapping
    public ResponseEntity<List<ApiKeySummaryResponse>> listApiKeys(
            @AuthenticationPrincipal UUID userId
    ) {
        List<ApiKeySummaryResponse> keys = apiKeyRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(k -> new ApiKeySummaryResponse(
                        k.getId(),
                        k.getName(),
                        k.getKeyValue().substring(0, 12) + "...",
                        k.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(keys);
    }

    /**
     * Revokes (soft-deletes) an API key. Any MCP client using this key will
     * immediately stop being able to authenticate.
     */
    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> revokeApiKey(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID keyId
    ) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .filter(k -> k.getUser().getId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        key.setActive(false);
        apiKeyRepository.save(key);

        return ResponseEntity.noContent().build();
    }
}
