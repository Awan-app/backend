package com.ezdo.controller;

import com.ezdo.dto.UpdateProfileRequest;
import com.ezdo.dto.UserProfileResponse;
import com.ezdo.dto.profile.*;
import com.ezdo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
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

    @GetMapping("/me/is-new")
    public ResponseEntity<Map<String, Boolean>> isNew(
        @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(userService.isNew(userId));
    }

    @PatchMapping(value = "/me/profile/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfilePictureResponse> updateProfilePicture(
        @AuthenticationPrincipal UUID userId,
        @RequestParam("image") MultipartFile image
    ) {
        return ResponseEntity.ok(userService.updateProfilePicture(userId, image));
    }

    @DeleteMapping("/me/profile/picture")
    public ResponseEntity<Void> removeProfilePicture(@AuthenticationPrincipal UUID userId) {
        userService.removeProfilePicture(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(@AuthenticationPrincipal UUID userId,
                                                             @Valid @RequestBody UpdateProfileRequest request) {
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

    // FOR TESTING PURPOSES ONLY — sets the user's point balance to an absolute
    // value. Not part of the production API.
    @PatchMapping("/me/points")
    public ResponseEntity<Map<String, Long>> setPoints(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody SetPointsRequest request
    ) {
        return ResponseEntity.ok(Map.of("points", userService.setPoints(userId, request.points())));
    }
}
