package com.authservice.Service;

import com.authservice.Exception.CustomException;
import com.authservice.Repository.UserRepository;
import com.authservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Logger log =
        LoggerFactory.getLogger(EmailVerificationService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final int OTP_EXPIRY_MINUTES = 10;

    // ─── Send Verification OTP ────────────────────────────────
    public void sendVerificationOtp(String email) {
        String otp = generateOtp();
        String redisKey = "verify:" + email;

        // Store OTP in Redis
        redisTemplate.opsForValue().set(
            redisKey, otp,
            Duration.ofMinutes(OTP_EXPIRY_MINUTES)
        );

        // Send email
        emailService.sendVerificationEmail(email, otp);
        log.info("Verification OTP sent to: {}", email);
    }

    // ─── Verify Email OTP ─────────────────────────────────────
    public String verifyEmail(String email, String otp) {

        String redisKey = "verify:" + email;
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        // Check OTP exists
        if (storedOtp == null) {
            log.warn("Verification OTP expired for: {}", email);
            throw new CustomException(
                "OTP has expired. Please request a new one.",
                HttpStatus.BAD_REQUEST);
        }

        // Check OTP matches
        if (!storedOtp.equals(otp)) {
            log.warn("Invalid verification OTP for: {}", email);
            throw new CustomException(
                "Invalid OTP. Please try again.",
                HttpStatus.BAD_REQUEST);
        }

        // Mark user as verified
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(
                    "User not found", HttpStatus.NOT_FOUND));

        user.setVerified(true);
        userRepository.save(user);

        // Delete OTP from Redis
        redisTemplate.delete(redisKey);

        log.info("Email verified successfully for: {}", email);
        return "Email verified successfully. You can now login!";
    }

    // ─── Resend Verification OTP ──────────────────────────────
    public String resendVerificationOtp(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(
                    "No account found with this email",
                    HttpStatus.NOT_FOUND));

        if (user.isVerified()) {
            throw new CustomException(
                "Email is already verified",
                HttpStatus.BAD_REQUEST);
        }

        sendVerificationOtp(email);
        return "Verification OTP resent to your email.";
    }

    // ─── Generate 6 digit OTP ─────────────────────────────────
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}