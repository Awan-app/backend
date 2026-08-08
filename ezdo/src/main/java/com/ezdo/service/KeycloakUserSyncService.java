package com.ezdo.service;

import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Manages user lifecycle in Keycloak for MCP OAuth authentication.
 * <p>
 * When a user registers in EZDO (via OTP or Firebase), this service
 * creates a corresponding user in the Keycloak "ezdo" realm so they
 * can authenticate via OAuth when connecting Claude Desktop / MCP clients.
 * <p>
 * Keycloak acts purely as the OAuth Authorization Server for MCP.
 * The app's own auth (OTP + Firebase + JWT) remains unchanged.
 */
@Slf4j
@Service
public class KeycloakUserSyncService {

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakUserSyncService(
            @Value("${ezdo.keycloak.admin-url:http://localhost:9090}") String serverUrl,
            @Value("${ezdo.keycloak.realm:ezdo}") String realm,
            @Value("${ezdo.keycloak.admin-client-id:admin-cli}") String clientId,
            @Value("${ezdo.keycloak.admin-username:admin}") String username,
            @Value("${ezdo.keycloak.admin-password:admin}") String password
    ) {
        this.realm = realm;
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .clientId(clientId)
                .username(username)
                .password(password)
                .build();
    }

    /**
     * Creates a user in Keycloak with the given email and a temporary password.
     * The user will be prompted to change the password on first login to Keycloak.
     * <p>
     * If the user already exists in Keycloak (e.g. from a previous registration),
     * this method logs a warning and returns silently.
     *
     * @param email         the user's email address
     */
    public void createUser(String email) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Check if user already exists in Keycloak
            List<UserRepresentation> existing = usersResource.searchByEmail(email, true);
            if (existing != null && !existing.isEmpty()) {
                log.debug("Keycloak user already exists for email: {}", email);
                return;
            }

            UserRepresentation user = new UserRepresentation();
            user.setEmail(email);
            user.setUsername(email);
            user.setEnabled(true);
            user.setEmailVerified(true);



            try (Response response = usersResource.create(user)) {
                int status = response.getStatus();
                if (status == 201) {
                    log.info("Created Keycloak user for email: {}", email);
                } else if (status == 409) {
                    log.warn("Keycloak user already exists (409) for email: {}", email);
                } else {
                    log.error("Failed to create Keycloak user for email: {}. Status: {}", email, status);
                }
            }
        } catch (Exception e) {
            // Don't let Keycloak failures break the registration flow
            log.error("Failed to sync user to Keycloak for email: {}. Error: {}", email, e.getMessage(), e);
        }
    }
}
