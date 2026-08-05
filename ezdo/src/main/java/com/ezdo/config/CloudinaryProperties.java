package com.ezdo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ezdo.cloudinary")
public record CloudinaryProperties(
    String cloudName,
    String apiKey,
    String apiSecret
) {
}
