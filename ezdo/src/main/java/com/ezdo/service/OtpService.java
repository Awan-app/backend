package com.ezdo.service;

import com.ezdo.dto.OtpResponse;
import com.ezdo.entity.VerificationCode;
import com.ezdo.exception.ErrorCodes;
import com.ezdo.exception.OtpRateLimitException;
import com.ezdo.exception.OtpVerificationException;
import com.ezdo.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;

    @Value("${ezdo.otp.pepper}")
    private String pepper;

    @Value("${ezdo.otp.expiry-seconds}")
    private int expirySeconds;

    @Value("${ezdo.otp.resend-cooldown-seconds}")
    private int resendCooldownSeconds;

    @Value("${ezdo.otp.max-attempts}")
    private int maxAttempts;

    @Value("${ezdo.otp.rate-limit-max}")
    private int rateLimitMax;

    @Value("${ezdo.otp.rate-limit-window-minutes}")
    private int rateLimitWindowMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public OtpResponse requestOtp(String email) {
        // Rate limit check: max N requests per email per M minutes
        Instant windowStart = Instant.now().minus(rateLimitWindowMinutes, ChronoUnit.MINUTES);
        long recentCount = verificationCodeRepository.countByEmailAndCreatedAtAfter(email, windowStart);
        if (recentCount >= rateLimitMax) {
            throw new OtpRateLimitException(rateLimitWindowMinutes * 60L);
        }

        // Invalidate any previous unconsumed codes for this email
        verificationCodeRepository.invalidateUnconsumedByEmail(email);

        // Generate 6-digit code (100000-999999)
        int rawCodeInt = 100_000 + secureRandom.nextInt(900_000);
        String rawCode = String.valueOf(rawCodeInt);

        // Hash the code with HMAC-SHA256 using server pepper
        String codeHash = hmacSha256(rawCode);

        // Save verification code entity
        VerificationCode vc = new VerificationCode();
        vc.setEmail(email);
        vc.setCodeHash(codeHash);
        vc.setExpiresAt(Instant.now().plusSeconds(expirySeconds));
        verificationCodeRepository.save(vc);

        // Send OTP email (raw code only leaves the server here)
        emailService.sendOtpEmail(email, rawCode);

        return new OtpResponse("ok", expirySeconds, resendCooldownSeconds);
    }

    @Transactional
    public VerificationCode verifyOtp(String email, String code) {
        // Find latest unconsumed, unlocked code for this email
        VerificationCode vc = verificationCodeRepository
                .findFirstByEmailAndConsumedFalseAndLockedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new OtpVerificationException(
                        "No valid OTP found for this email. Please request a new one.",
                        ErrorCodes.OTP_EXPIRED_OR_NOT_FOUND
                ));

        // Check expiry
        if (vc.getExpiresAt().isBefore(Instant.now())) {
            throw new OtpVerificationException(
                    "OTP has expired. Please request a new one.",
                    ErrorCodes.OTP_EXPIRED_OR_NOT_FOUND
            );
        }

        // Compare HMAC hash of submitted code against stored hash
        String submittedHash = hmacSha256(code);
        if (!submittedHash.equals(vc.getCodeHash())) {
            vc.setAttempts(vc.getAttempts() + 1);
            if (vc.getAttempts() >= maxAttempts) {
                vc.setLocked(true);
                verificationCodeRepository.save(vc);
                throw new OtpVerificationException(
                        "Too many failed attempts. This OTP is now locked. Please request a new one.",
                        ErrorCodes.OTP_LOCKED
                );
            }
            verificationCodeRepository.save(vc);
            throw new OtpVerificationException(
                    "Invalid OTP code.",
                    ErrorCodes.OTP_INVALID_CODE,
                    Map.of("remaining_attempts", maxAttempts - vc.getAttempts())
            );
        }

        // Success — mark consumed
        vc.setConsumed(true);
        verificationCodeRepository.save(vc);
        return vc;
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }
}
