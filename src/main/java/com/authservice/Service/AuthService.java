package com.authservice.Service;

import com.authservice.dto.AuthResponse;
import com.authservice.dto.LoginRequest;
import com.authservice.dto.RegisterRequest;
import com.authservice.entity.User;
import com.authservice.entity.Role;
import com.authservice.Exception.CustomException;
import com.authservice.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTokenService redisService;
    private final AuthenticationManager authenticationManager;
    private final RateLimitService rateLimitService;
    private final EmailVerificationService emailVerificationService;
    private final AccountLockoutService accountLockoutService;

    // ─── Update register method ───────────────────────────────
public String register(RegisterRequest request, String ipAddress) {

    rateLimitService.checkRegisterRateLimit(ipAddress);
    log.info("Registration attempt for: {}", request.getUsername());

    if (userRepository.existsByUsername(request.getUsername())) {
        throw new CustomException(
            "Username already taken", HttpStatus.CONFLICT);
    }

    if (userRepository.existsByEmail(request.getEmail())) {
        throw new CustomException(
            "Email already registered", HttpStatus.CONFLICT);
    }

    // Save user as unverified
    User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(Role.USER)
            .isVerified(false)
            .build();

    userRepository.save(user);
    log.info("User registered (unverified): {}", request.getUsername());

    // Send verification OTP
    emailVerificationService.sendVerificationOtp(request.getEmail());

    return "Registration successful! " +
           "Please check your email for verification OTP.";
}


    public AuthResponse login(LoginRequest request, String ipAddress) {

    rateLimitService.checkLoginRateLimit(ipAddress);
    accountLockoutService.checkAccountLocked(request.getUsername());

    log.info("Login attempt for: {}", request.getUsername());

    try {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );
    } catch (BadCredentialsException e) {
        accountLockoutService.handleFailedLogin(request.getUsername());
        throw new CustomException(
            "Invalid username or password", HttpStatus.UNAUTHORIZED);
    }

    User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new CustomException(
                "User not found", HttpStatus.NOT_FOUND));

    // Check email verification
    if (!user.isVerified()) {
        log.warn("Unverified login attempt: {}", request.getUsername());
        throw new CustomException(
            "Please verify your email before logging in. " +
            "Check your inbox for the OTP.",
            HttpStatus.FORBIDDEN);
    }

    accountLockoutService.resetFailedAttempts(request.getUsername());
    rateLimitService.resetLoginRateLimit(ipAddress);

    String accessToken = jwtService.generateAccessToken(user.getUsername());
    String refreshToken = jwtService.generateRefreshToken(user.getUsername());

    redisService.saveToken(
        "refresh:" + user.getUsername(), refreshToken, 604800000L);

    log.info("Login successful: {}", request.getUsername());

    return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .username(user.getUsername())
            .email(user.getEmail())
            .build();
}

    public String logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Logout failed - invalid Authorization header");
            throw new CustomException("Invalid token", HttpStatus.BAD_REQUEST);
        }

        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);

        redisService.blacklistToken(token, 900000L);
        redisService.deleteToken("refresh:" + username);

        log.info("User logged out: {}", username);
        return "Logged out successfully";
    }

    public AuthResponse refreshToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Token refresh failed - invalid header");
            throw new CustomException("Invalid token", HttpStatus.BAD_REQUEST);
        }

        String refreshToken = authHeader.substring(7);
        String username = jwtService.extractUsername(refreshToken);

        log.info("Token refresh attempt for: {}", username);

        String storedToken = redisService.getToken("refresh:" + username);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            log.warn("Token refresh failed - invalid token for: {}", username);
            throw new CustomException(
                "Refresh token invalid or expired", HttpStatus.UNAUTHORIZED);
        }

        String newAccessToken = jwtService.generateAccessToken(username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(
                    "User not found", HttpStatus.NOT_FOUND));

        log.info("Token refreshed for: {}", username);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
