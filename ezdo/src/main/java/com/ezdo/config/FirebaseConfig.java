package com.ezdo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {

    // Default to empty strings if missing from YML
    @Value("${ezdo.firebase.service-account-path:}")
    private String credentialsPath;

    @Value("${ezdo.firebase.credentials-json:}")
    private String credentialsJson;

    @Autowired
    private ResourceLoader resourceLoader;

    @PostConstruct
    public void initialize() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            InputStream rawStream = resolveCredentialsStream();

            if (rawStream != null) {
                // Ensure stream closes automatically after credentials read
                try (InputStream serviceAccountStream = rawStream) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                            .build();

                    FirebaseApp.initializeApp(options);
                    log.info("Firebase Admin SDK initialized successfully.");
                }
            } else {
                log.warn("No Firebase credentials provided. Push notifications will be disabled.");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
        }
    }

    private InputStream resolveCredentialsStream() throws IOException {
        // 1. High priority: Direct JSON content (e.g. env variable in Docker/K8s)
        if (credentialsJson != null && !credentialsJson.trim().isEmpty()) {
            log.info("Initializing Firebase using credentials-json from environment.");
            return new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
        }

        // 2. Fallback: Path (handles classpath: or filesystem path via ResourceLoader)
        if (credentialsPath != null && !credentialsPath.trim().isEmpty()) {
            log.info("Initializing Firebase using resource path: {}", credentialsPath);
            return resourceLoader.getResource(credentialsPath).getInputStream();
        }

        return null;
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("FirebaseApp is not initialized; FirebaseMessaging bean is unavailable.");
            return null;
        }
        return FirebaseMessaging.getInstance();
    }
}
