package com.ezdo.controller;

import com.ezdo.config.McpJwtAuthenticationConverter;
import io.micrometer.observation.Observation;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.CoyoteAdapter;
import org.apache.catalina.core.ApplicationFilterChain;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.coyote.AbstractProcessorLight;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.http11.Http11Processor;
import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.net.SocketProcessorBase;
import org.apache.tomcat.util.threads.TaskThread;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.ObservationAuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.OAuth2ProtectedResourceMetadataFilter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.ObservationFilterChainDecorator;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.*;

import java.util.List;
import java.util.Map;

@RestController
public class McpOAuthMetadataController {

    @Value("${ezdo.keycloak.issuer-uri}")
    private String keycloakIssuerUri;

    @GetMapping("/.well-known/oauth-authorization-server")
    public Map<String, Object> oauthMetadata() {
        return Map.of(
            "issuer", keycloakIssuerUri,
            "authorization_endpoint", keycloakIssuerUri + "/protocol/openid-connect/auth",
            "token_endpoint", keycloakIssuerUri + "/protocol/openid-connect/token",
            "device_authorization_endpoint", keycloakIssuerUri + "/protocol/openid-connect/auth/device",
            "jwks_uri", keycloakIssuerUri + "/protocol/openid-connect/certs",
            "response_types_supported", List.of("code"),
            "grant_types_supported", List.of(
                "authorization_code",
                "urn:ietf:params:oauth:grant-type:device_code",
                "refresh_token"
            ),
            "code_challenge_methods_supported", List.of("S256"),
            "scopes_supported", List.of("openid", "email", "profile")
        );
    }

    @GetMapping({ "/.well-known/oauth-protected-resource", "/.well-known/oauth-protected-resource/mcp" })
    public Map<String, Object> protectedResourceMetadata() {
        return Map.of(
            "resource", keycloakIssuerUri, // often the audience
            "authorization_servers", List.of(keycloakIssuerUri),
            "scopes_supported", List.of("openid", "email", "profile"),
            "bearer_methods_supported", List.of("header")
        );
    }
}

