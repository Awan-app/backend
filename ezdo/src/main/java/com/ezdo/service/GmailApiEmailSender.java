package com.ezdo.service;

import com.ezdo.config.MailProperties;
import com.ezdo.exception.EmailDeliveryException;
import com.google.auth.oauth2.UserCredentials;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
@ConditionalOnProperty(name = "ezdo.mail.provider", havingValue = "gmail")
public class GmailApiEmailSender implements EmailSender {

    private final UserCredentials credentials;
    private final RestClient restClient;
    private final String fromAddress;

    public GmailApiEmailSender(
        UserCredentials credentials,
        @Qualifier("gmailRestClient") RestClient restClient,
        MailProperties props
    ) {
        this.credentials = credentials;
        this.restClient = restClient;
        this.fromAddress = props.from();
    }

    @Override
    public void send(String toEmail, String subject, String htmlBody) {
        String raw = buildRawMessage(toEmail, subject, htmlBody);
        String accessToken = accessToken();
        try {
            restClient.post()
                .uri("/users/me/messages/send")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("raw", raw))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("Gmail API rejected the message to {}: {} {}",
                        toEmail, response.getStatusCode().value(), body);
                    throw new EmailDeliveryException();
                })
                .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Gmail API call failed for {}", toEmail, e);
            throw new EmailDeliveryException(e);
        }
    }

    private String accessToken() {
        try {
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            log.error("Failed to obtain a Gmail access token", e);
            throw new EmailDeliveryException(e);
        }
    }

    private String buildRawMessage(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = new MimeMessage(jakarta.mail.Session.getInstance(new Properties()));
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            message.writeTo(buffer);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());
        } catch (MessagingException | IOException e) {
            log.error("Failed to build the MIME message for {}", toEmail, e);
            throw new EmailDeliveryException(e);
        }
    }
}
