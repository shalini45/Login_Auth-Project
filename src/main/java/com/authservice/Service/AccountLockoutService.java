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

@Service
@RequiredArgsConstructor
public class AccountLockoutService {

    private static final Logger log =
        LoggerFactory.getLogger(AccountLockoutService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    // Max failed attempts before lockout
    private static final int MAX_FAILED_ATTEMPTS = 5;

    // Lockout duration in minutes
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    // ─── Check if account is locked ──────────────────────────
    public void checkAccountLocked(String username) {
        String lockKey = "lockout:" + username;
        String lockedValue = redisTemplate.opsForValue().get(lockKey);

        if (lockedValue != null) {
            Long ttl = redisTemplate.getExpire(lockKey);
            log.warn("Locked account login attempt: {}", username);
            throw new CustomException(
                "Account is locked due to too many failed attempts. " +
                "Try again after " + ttl + " seconds.",
                HttpStatus.LOCKED
            );
        }
    }

    // ─── Handle failed login attempt ─────────────────────────
    public void handleFailedLogin(String username) {
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) return;

        int newFailedAttempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(newFailedAttempts);

        log.warn("Failed login attempt {}/{} for username: {}",
                newFailedAttempts, MAX_FAILED_ATTEMPTS, username);

        if (newFailedAttempts >= MAX_FAILED_ATTEMPTS) {
            // Lock the account in Redis
            String lockKey = "lockout:" + username;
            redisTemplate.opsForValue().set(
                lockKey,
                "locked",
                Duration.ofMinutes(LOCKOUT_DURATION_MINUTES)
            );
            user.setAccountLocked(true);
            userRepository.save(user);

            log.warn("Account LOCKED for {} minutes: {}",
                    LOCKOUT_DURATION_MINUTES, username);

            throw new CustomException(
                "Account locked for " + LOCKOUT_DURATION_MINUTES +
                " minutes due to too many failed attempts.",
                HttpStatus.LOCKED
            );
        }

        userRepository.save(user);

        // Warn user how many attempts remaining
        int remaining = MAX_FAILED_ATTEMPTS - newFailedAttempts;
        log.info("Failed attempts remaining before lockout: {}", remaining);
    }

    // ─── Reset failed attempts on successful login ────────────
    public void resetFailedAttempts(String username) {
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) return;

        if (user.getFailedAttempts() > 0 || user.isAccountLocked()) {
            user.setFailedAttempts(0);
            user.setAccountLocked(false);
            user.setLockTime(null);
            userRepository.save(user);

            // Remove lock from Redis if exists
            redisTemplate.delete("lockout:" + username);

            log.info("Failed attempts reset for username: {}", username);
        }
    }
}