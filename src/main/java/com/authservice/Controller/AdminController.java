package com.authservice.Controller;

import com.authservice.Exception.CustomException;
import com.authservice.Repository.UserRepository;
import com.authservice.entity.Role;
import com.authservice.entity.User;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Logger log =
        LoggerFactory.getLogger(AdminController.class);

    private final UserRepository userRepository;
    @Tag(name = "Admin", description = "Admin only APIs")


    // ─── Get all users (ADMIN only) ───────────────────────────
    @Operation(summary = "Get all users", description = "Returns list of all users. ADMIN only.")

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        log.info("Admin fetching all users");

        List<Map<String, Object>> users = userRepository.findAll()
                .stream()
                .map(user -> Map.of(
                    "id", (Object) user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole().name(),
                    "accountLocked", user.isAccountLocked(),
                    "createdAt", user.getCreatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(users);
    }

    // ─── Delete user (ADMIN only) ─────────────────────────────

    @Operation(summary = "Delete user", description = "Deletes a user by ID. ADMIN only.")

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable Long id) {
        log.info("Admin deleting user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                    "User not found", HttpStatus.NOT_FOUND));

        userRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());

        return ResponseEntity.ok(Map.of(
            "message", "User deleted successfully"
        ));
    }

    // ─── Change user role (ADMIN only) ────────────────────────

    @Operation(summary = "Change user role", description = "Changes role of a user. ADMIN only.")

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> changeUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                    "User not found", HttpStatus.NOT_FOUND));

        String newRole = request.get("role").toUpperCase();
        user.setRole(Role.valueOf(newRole));
        userRepository.save(user);

        log.info("Role changed to {} for user: {}", newRole, user.getUsername());

        return ResponseEntity.ok(Map.of(
            "message", "Role updated to " + newRole +
                       " for user: " + user.getUsername()
        ));
    }

    // ─── Unlock user account (ADMIN only) ────────────────────

    @Operation(summary = "Unlock account", description = "Unlocks a locked user account. ADMIN only.")

    @PutMapping("/users/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> unlockAccount(
            @PathVariable Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                    "User not found", HttpStatus.NOT_FOUND));

        user.setAccountLocked(false);
        user.setFailedAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);

        log.info("Account unlocked by admin for user: {}", user.getUsername());

        return ResponseEntity.ok(Map.of(
            "message", "Account unlocked for: " + user.getUsername()
        ));
    }
}