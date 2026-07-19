package com.ezdo.controller;

import com.ezdo.dto.UpdateProfileRequest;
import com.ezdo.dto.UserProfileResponse;
import com.ezdo.dto.profile.*;
import com.ezdo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(@AuthenticationPrincipal UUID userId, @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PatchMapping("/me/profile/name")
    public ResponseEntity<UserProfileResponse> updateName(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody UpdateNameRequest request
    ) {
        return ResponseEntity.ok(userService.updateName(userId, request));
    }

    @PatchMapping("/me/profile/birth-date")
    public ResponseEntity<UserProfileResponse> updateBirthDate(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody UpdateBirthDateRequest request
    ) {
        return ResponseEntity.ok(userService.updateBirthDate(userId, request));
    }

    @PatchMapping("/me/streak/increment")
    public ResponseEntity<UserProgressResponse> incrementStreak(
        @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(userService.incrementStreak(userId));
    }

    @PatchMapping("/me/streak/reset")
    public ResponseEntity<UserProgressResponse> resetStreak(
        @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(userService.resetStreak(userId));
    }

    @PatchMapping("/me/points/award")
    public ResponseEntity<UserProgressResponse> awardPoints(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody AwardPointsRequest request
    ) {
        return ResponseEntity.ok(userService.awardPoints(userId, request));
    }

    @PatchMapping("/me/points/deduct")
    public ResponseEntity<UserProgressResponse> deductPoints(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody DeductPointsRequest request
        ) {
        return ResponseEntity.ok(userService.deductPoints(userId, request));
    }

    @PatchMapping("/me/preferences/timezone")
    public ResponseEntity<UserProfileResponse> updateTimezone(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody UpdateTimezoneRequest request
    ) {
        return ResponseEntity.ok(userService.updateTimezone(userId, request));
    }

    @PatchMapping("/me/preferences/session")
    public ResponseEntity<UserProfileResponse> updateSessionSettings(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody UpdateSessionSettingsRequest request
    ) {
        return ResponseEntity.ok(userService.updateSessionSettings(userId, request));
    }

    @PatchMapping("/me/preferences/sleep-schedule")
    public ResponseEntity<UserProfileResponse> updateSleepSchedule(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody UpdateSleepScheduleRequest request
    ) {
        return ResponseEntity.ok(userService.updateSleepSchedule(userId, request));
    }

    @PatchMapping("/me/preferences/scheduling-type")
    public ResponseEntity<UserProfileResponse> updateSchedulingType(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody UpdateSchedulingTypeRequest request
    ) {
        return ResponseEntity.ok(userService.updateSchedulingType(userId, request));
    }
}
