package com.ezdo.config;

import com.google.auth.oauth2.UserCredentials;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    @Bean
    @ConditionalOnProperty(name = "ezdo.mail.provider", havingValue = "gmail")
    public UserCredentials gmailCredentials(MailProperties props) {
        MailProperties.Gmail gmail = props.gmail();
        if (!StringUtils.hasText(gmail.clientId())
                || !StringUtils.hasText(gmail.clientSecret())
                || !StringUtils.hasText(gmail.refreshToken())) {
            throw new IllegalStateException(
                "ezdo.mail.provider=gmail requires ezdo.mail.gmail.client-id, client-secret and refresh-token");
        }
        return UserCredentials.newBuilder()
            .setClientId(gmail.clientId())
            .setClientSecret(gmail.clientSecret())
            .setRefreshToken(gmail.refreshToken())
            .build();
    }

    @Bean
    @ConditionalOnProperty(name = "ezdo.mail.provider", havingValue = "gmail")
    public RestClient gmailRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder()
            .baseUrl("https://gmail.googleapis.com/gmail/v1")
            .requestFactory(factory)
            .build();
    }
}
