package com.ezdo.service;

import com.ezdo.repository.RefreshTokenRepository;
import com.ezdo.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthCleanupScheduler {

    private final VerificationCodeRepository verificationCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @Transactional
    public void purgeExpired() {
        Instant now = Instant.now();
        int codes = verificationCodeRepository.deleteExpired(now);
        int tokens = refreshTokenRepository.deleteExpired(now);
        log.info("Auth cleanup: deleted {} expired verification codes, {} expired refresh tokens", codes, tokens);
    }
}
