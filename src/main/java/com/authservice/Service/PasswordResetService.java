package com.authservice.Service;

import com.authservice.Exception.CustomException;
import com.authservice.Repository.UserRepository;
import com.authservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log =
        LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // OTP expiry in minutes
    private static final int OTP_EXPIRY_MINUTES = 10;

    // ─── Step 1: Generate and Send OTP ───────────────────────
    public String forgotPassword(String email) {

        // Check if email exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(
                    "No account found with this email",
                    HttpStatus.NOT_FOUND));

        // Generate 6 digit OTP
        String otp = generateOtp();

        // Store OTP in Redis with 10 min expiry
        String redisKey = "otp:" + email;
        redisTemplate.opsForValue().set(
            redisKey, otp,
            Duration.ofMinutes(OTP_EXPIRY_MINUTES)
        );

        // Send OTP via email
        emailService.sendOtpEmail(email, otp);

        log.info("OTP generated and sent for email: {}", email);

        return "OTP sent to your email. Valid for " +
               OTP_EXPIRY_MINUTES + " minutes.";
    }

    // ─── Step 2: Validate OTP and Reset Password ──────────────
    public String resetPassword(String email,
                                 String otp,
                                 String newPassword) {

        // Get OTP from Redis
        String redisKey = "otp:" + email;
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        // Check if OTP exists
        if (storedOtp == null) {
            log.warn("OTP expired or not found for: {}", email);
            throw new CustomException(
                "OTP has expired. Please request a new one.",
                HttpStatus.BAD_REQUEST);
        }

        // Check if OTP matches
        if (!storedOtp.equals(otp)) {
            log.warn("Invalid OTP attempt for: {}", email);
            throw new CustomException(
                "Invalid OTP. Please try again.",
                HttpStatus.BAD_REQUEST);
        }

        // Get user and update password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(
                    "User not found", HttpStatus.NOT_FOUND));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete OTP from Redis after use
        redisTemplate.delete(redisKey);

        log.info("Password reset successful for: {}", email);

        return "Password reset successfully. Please login with new password.";
    }

    // ─── Generate 6 digit OTP ─────────────────────────────────
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}