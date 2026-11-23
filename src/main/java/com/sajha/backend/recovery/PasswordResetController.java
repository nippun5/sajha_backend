package com.sajha.backend.recovery;

import com.sajha.backend.security.RateLimitService;
import com.sajha.backend.util.SecurityStrings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth/password")
public class PasswordResetController {
    private final PasswordResetService recovery;
    private final RateLimitService limiter;
    public PasswordResetController(PasswordResetService recovery, RateLimitService limiter) {
        this.recovery = recovery;
        this.limiter = limiter;
    }
    @PostMapping("/forgot")
    public ResponseEntity<PasswordResetDtos.Accepted> forgot(
        @Valid @RequestBody PasswordResetDtos.Forgot input, HttpServletRequest request) {
        String email = SecurityStrings.normalizeEmail(input.email());
        limiter.check("password-forgot-ip:" + request.getRemoteAddr(), 5, Duration.ofHours(1));
        limiter.check("password-forgot-email:" + SecurityStrings.sha256(email), 3, Duration.ofHours(1));
        recovery.request(email);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            new PasswordResetDtos.Accepted("If the account exists, follow the recovery instructions."));
    }
    @PostMapping("/reset")
    public ResponseEntity<Void> reset(
        @Valid @RequestBody PasswordResetDtos.Redeem input, HttpServletRequest request) {
        limiter.check("password-reset-ip:" + request.getRemoteAddr(), 15, Duration.ofMinutes(15));
        recovery.redeem(input.token(), input.newPassword());
        return ResponseEntity.noContent().build();
    }
}
