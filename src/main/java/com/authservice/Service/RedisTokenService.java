package com.authservice.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisTokenService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void saveToken(String key, String value, long expirationMs) {
        redisTemplate.opsForValue().set(key, value, expirationMs, TimeUnit.MILLISECONDS);
    }

    public String getToken(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteToken(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasToken(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void blacklistToken(String token, long expirationMs) {
        redisTemplate.opsForValue().set(
            "blacklist:" + token, "true", expirationMs, TimeUnit.MILLISECONDS
        );
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }
}