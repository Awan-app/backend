package com.ezdo.service;

import com.ezdo.dto.email.MorningSummaryEmail;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private final EmailSender emailSender;
    private final TemplateEngine templateEngine;

    public EmailService(EmailSender emailSender, TemplateEngine templateEngine) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
    }

    public void sendOtpEmail(String toEmail, String code) {
        Context context = new Context();
        context.setVariable("code", code);
        String htmlContent = templateEngine.process("email/otp-email", context);
        emailSender.send(toEmail, "Your EZDO verification code", htmlContent);
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
        emailSender.send(toEmail, subject, html);
    }
}
