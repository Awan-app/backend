package com.ezdo.controller;

import com.ezdo.dto.*;
import com.ezdo.service.AuthService;
import com.ezdo.service.FirebaseAuthService;
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
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final FirebaseAuthService firebaseAuthService;

    @PostMapping("/otp/request")
    public ResponseEntity<OtpResponse> requestOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.ok(authService.requestOtp(request));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    /** Google sign-in: the client signs in with Firebase and posts the resulting ID token here. */
    @PostMapping("/firebase")
    public ResponseEntity<AuthResponse> firebaseLogin(@Valid @RequestBody FirebaseLoginRequest request) {
        return ResponseEntity.ok(firebaseAuthService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UUID userId, @Valid @RequestBody LogoutRequest request) {
        authService.logout(userId, request);
        return ResponseEntity.noContent().build();
    }
}
