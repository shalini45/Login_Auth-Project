package com.authservice.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // Max attempts allowed
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int MAX_REGISTER_ATTEMPTS = 3;

    // Time window in seconds
    private static final int LOGIN_WINDOW_SECONDS = 60;
    private static final int REGISTER_WINDOW_SECONDS = 60;

    // ─── Check Login Rate Limit ───────────────────────────────
    public void checkLoginRateLimit(String ipAddress) {
        String key = "ratelimit:login:" + ipAddress;
        checkRateLimit(key, MAX_LOGIN_ATTEMPTS, LOGIN_WINDOW_SECONDS, "login");
    }

    // ─── Check Register Rate Limit ────────────────────────────
    public void checkRegisterRateLimit(String ipAddress) {
        String key = "ratelimit:register:" + ipAddress;
        checkRateLimit(key, MAX_REGISTER_ATTEMPTS, REGISTER_WINDOW_SECONDS, "register");
    }

    // ─── Core Rate Limit Logic ────────────────────────────────
    private void checkRateLimit(String key, int maxAttempts,
                                 int windowSeconds, String action) {

        // Get current count from Redis
        String currentCountStr = redisTemplate.opsForValue().get(key);
        int currentCount = currentCountStr != null ?
                           Integer.parseInt(currentCountStr) : 0;

        log.debug("Rate limit check - key: {}, count: {}/{}", 
                  key, currentCount, maxAttempts);

        if (currentCount >= maxAttempts) {
            // Get remaining TTL
            Long ttl = redisTemplate.getExpire(key);
            log.warn("Rate limit exceeded for {} - key: {}", action, key);
            throw new com.authservice.Exception.CustomException(
                "Too many " + action + " attempts. " +
                "Please try again after " + ttl + " seconds.",
                org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
            );
        }

        // Increment counter
        if (currentCount == 0) {
            // First attempt - set with expiry
            redisTemplate.opsForValue().set(
                key, "1", Duration.ofSeconds(windowSeconds)
            );
        } else {
            // Increment existing counter
            redisTemplate.opsForValue().increment(key);
        }

        log.debug("Rate limit count updated - key: {}, new count: {}",
                  key, currentCount + 1);
    }

    // ─── Reset Rate Limit on Successful Login ─────────────────
    public void resetLoginRateLimit(String ipAddress) {
        String key = "ratelimit:login:" + ipAddress;
        redisTemplate.delete(key);
        log.debug("Rate limit reset for IP: {}", ipAddress);
    }
}