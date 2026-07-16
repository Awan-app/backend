package com.ezdo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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
        try {
            Context context = new Context();
            context.setVariable("code", code);
            String htmlContent = templateEngine.process("email/otp-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8"); // true = multipart, needed for embedded image

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Your EZDO verification code");
            helper.setText(htmlContent, true); // true = this is HTML, not plain text

//            helper.addInline("ezdoLogo", new ClassPathResource("static/images/logo.png")); // matches cid:ezdoLogo above

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}