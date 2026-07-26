package com.ezdo.service;

import com.ezdo.dto.email.MorningSummaryEmail;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${ezdo.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendOtpEmail(String toEmail, String code) {
        Context context = new Context();
        context.setVariable("code", code);
        String htmlContent = templateEngine.process("email/otp-email", context);
        sendHtmlEmail(toEmail, "Your EZDO verification code", htmlContent);
    }

    public void sendDailySummaryEmail(String toEmail, MorningSummaryEmail email) {
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("hasSessions", !email.sessions().isEmpty());
        context.setVariable("hasGoals", !email.activeGoals().isEmpty());
        context.setVariable("hasDeadlines", !email.upcomingDeadlines().isEmpty());

        String subject = email.sessions().isEmpty()
                ? "Your day is clear today"
                : "Your plan for today";

        String html = templateEngine.process("email/daily-summary-email", context);
        sendHtmlEmail(toEmail, subject, html);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to " + toEmail, e);
        }
    }
}