package com.ezdo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ezdo.mail")
public record MailProperties(
    @DefaultValue("smtp") String provider,
    @DefaultValue("noreply@awan.com") String from,
    @DefaultValue Gmail gmail
) {

    public record Gmail(
        @DefaultValue("") String clientId,
        @DefaultValue("") String clientSecret,
        @DefaultValue("") String refreshToken
    ) {
    }
}
