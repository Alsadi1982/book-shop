package com.otus.user_service.rest;

import com.otus.user_service.dto.*;
import com.otus.user_service.service.UserService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    @CircuitBreaker(name = "userService", fallbackMethod = "registerFallback")
    @RateLimiter(name = "userService")
    @Retry(name = "userService")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        logger.info("POST /api/users/register - username: {}", registrationDTO.getUsername());

        try {
            UserResponseDTO user = userService.registerUser(registrationDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (RuntimeException e) {
            logger.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public ResponseEntity<?> registerFallback(UserRegistrationDTO registrationDTO, Throwable t) {
        logger.error("Registration fallback: {}", t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Registration temporarily unavailable",
                        "reason", "Service overloaded or unavailable"
                ));
    }

    @PostMapping("/login")
    @CircuitBreaker(name = "userService", fallbackMethod = "loginFallback")
    @RateLimiter(name = "userService")
    @Retry(name = "userService")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        logger.info("POST /api/users/login - user: {}", loginDTO.getUsernameOrEmail());

        LoginResponseDTO response = userService.login(loginDTO);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(401).body(response);
    }

    public ResponseEntity<?> loginFallback(UserLoginDTO loginDTO, Throwable t) {
        logger.error("Login fallback: {}", t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Login temporarily unavailable",
                        "reason", t.getMessage()
                ));
    }

    @GetMapping("/{id}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    @RateLimiter(name = "userService")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable Long id) {
        logger.info("GET /api/users/{}", id);

        UserResponseDTO user = userService.getUser(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> getUserFallback(Long id, Throwable t) {
        logger.error("Get user fallback for id {}: {}", id, t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service temporarily unavailable",
                        "reason", t.getMessage()
                ));
    }

    @GetMapping("/email/{email}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailFallback")
    @RateLimiter(name = "userService")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email) {
        logger.info("GET /api/users/email/{}", email);

        UserResponseDTO user = userService.getUserByEmail(email);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> getUserByEmailFallback(String email, Throwable t) {
        logger.error("Get user by email fallback for {}: {}", email, t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service temporarily unavailable",
                        "reason", t.getMessage()
                ));
    }

    @GetMapping
    @CircuitBreaker(name = "userService", fallbackMethod = "getAllUsersFallback")
    @RateLimiter(name = "userService")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        logger.info("GET /api/users");

        return ResponseEntity.ok(userService.getAllUsers());
    }

    public ResponseEntity<?> getAllUsersFallback(Throwable t) {
        logger.error("Get all users fallback: {}", t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot fetch users at this moment",
                        "reason", t.getMessage()
                ));
    }

    @PutMapping("/{id}")
    @CircuitBreaker(name = "userService", fallbackMethod = "updateUserFallback")
    @RateLimiter(name = "userService")
    @Retry(name = "userService")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO updateDTO) {
        logger.info("PUT /api/users/{}", id);

        try {
            UserResponseDTO user = userService.updateUser(id, updateDTO);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            logger.error("Update failed: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<?> updateUserFallback(Long id, UserUpdateDTO updateDTO, Throwable t) {
        logger.error("Update user fallback for id {}: {}", id, t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot update user at this moment",
                        "reason", t.getMessage()
                ));
    }

    @PostMapping("/{id}/change-password")
    @CircuitBreaker(name = "userService", fallbackMethod = "changePasswordFallback")
    @RateLimiter(name = "userService")
    @Retry(name = "userService")
    public ResponseEntity<?> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        logger.info("POST /api/users/{}/change-password", id);

        try {
            userService.changePassword(id, changePasswordDTO);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public ResponseEntity<?> changePasswordFallback(Long id, ChangePasswordDTO changePasswordDTO, Throwable t) {
        logger.error("Change password fallback for id {}: {}", id, t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot change password at this moment",
                        "reason", t.getMessage()
                ));
    }

    @PostMapping("/{id}/deactivate")
    @CircuitBreaker(name = "userService", fallbackMethod = "deactivateUserFallback")
    @RateLimiter(name = "userService")
    @Retry(name = "userService")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        logger.info("POST /api/users/{}/deactivate", id);

        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<?> deactivateUserFallback(Long id, Throwable t) {
        logger.error("Deactivate user fallback for id {}: {}", id, t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot deactivate user at this moment",
                        "reason", t.getMessage()
                ));
    }

    @PostMapping("/{id}/activate")
    @CircuitBreaker(name = "userService", fallbackMethod = "activateUserFallback")
    @RateLimiter(name = "userService")
    @Retry(name = "userService")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        logger.info("POST /api/users/{}/activate", id);

        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<?> activateUserFallback(Long id, Throwable t) {
        logger.error("Activate user fallback for id {}: {}", id, t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot activate user at this moment",
                        "reason", t.getMessage()
                ));
    }

    @DeleteMapping("/cache")
    @CircuitBreaker(name = "userService", fallbackMethod = "clearCacheFallback")
    @RateLimiter(name = "userService")
    public ResponseEntity<Void> clearCache() {
        logger.info("DELETE /api/users/cache");

        userService.clearCache();
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<?> clearCacheFallback(Throwable t) {
        logger.error("Clear cache fallback: {}", t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot clear cache at this moment",
                        "reason", t.getMessage()
                ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "user-service"));
    }

}
