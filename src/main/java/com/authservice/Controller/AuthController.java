package com.authservice.Controller;

import com.authservice.dto.AuthResponse;
import com.authservice.dto.LoginRequest;
import com.authservice.dto.RegisterRequest;
import com.authservice.Service.AuthService;
import com.authservice.Service.EmailVerificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.authservice.dto.ForgotPasswordRequest;
import com.authservice.dto.ResetPasswordRequest;
import com.authservice.Service.PasswordResetService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;



    // ─── Helper to get IP ─────────────────────────────────────
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) return request.getRemoteAddr();
        return xfHeader.split(",")[0];
    }

   @PostMapping("/register")
public ResponseEntity<Map<String, String>> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpRequest) {
    String ip = getClientIP(httpRequest);
    String message = authService.register(request, ip);
    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(Map.of("message", message));
}

// ─── Verify Email ─────────────────────────────────────────
@PostMapping("/verify-email")
public ResponseEntity<Map<String, String>> verifyEmail(
        @RequestBody Map<String, String> request) {
    String message = emailVerificationService.verifyEmail(
        request.get("email"),
        request.get("otp")
    );
    return ResponseEntity.ok(Map.of("message", message));
}

// ─── Resend Verification OTP ──────────────────────────────
@PostMapping("/resend-verification")
public ResponseEntity<Map<String, String>> resendVerification(
        @RequestBody Map<String, String> request) {
    String message = emailVerificationService
                        .resendVerificationOtp(request.get("email"));
    return ResponseEntity.ok(Map.of("message", message));
}

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIP(httpRequest);
        return ResponseEntity.ok(authService.login(request, ip));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(
            Map.of("message", authService.logout(authHeader)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(authService.refreshToken(authHeader));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Auth Service"
        ));
    }

    // ─── FORGOT PASSWORD ─────────────────────────────────────
@PostMapping("/forgot-password")
public ResponseEntity<Map<String, String>> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request) {
    String message = passwordResetService
                        .forgotPassword(request.getEmail());
    return ResponseEntity.ok(Map.of("message", message));
}

// ─── RESET PASSWORD ──────────────────────────────────────
@PostMapping("/reset-password")
public ResponseEntity<Map<String, String>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request) {
    String message = passwordResetService.resetPassword(
        request.getEmail(),
        request.getOtp(),
        request.getNewPassword()
    );
    return ResponseEntity.ok(Map.of("message", message));
}
}