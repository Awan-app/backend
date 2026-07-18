package com.ezdo.controller;

import com.ezdo.dto.OnboardingRequest;
import com.ezdo.dto.UserProfileResponse;
import com.ezdo.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping
    public ResponseEntity<UserProfileResponse> completeOnboarding(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody OnboardingRequest request
    ) {
        return ResponseEntity.ok(onboardingService.completeOnboarding(userId, request));
    }
}
