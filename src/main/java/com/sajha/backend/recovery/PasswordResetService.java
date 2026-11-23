package com.sajha.backend.recovery;

import com.sajha.backend.api.ApiProblem;
import com.sajha.backend.auth.RefreshSessionRepository;
import com.sajha.backend.user.UserRepository;
import com.sajha.backend.util.SecurityStrings;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PasswordResetService {
    private final PasswordResetTokenRepository tokens;
    private final UserRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final PasswordResetDelivery delivery;

    public PasswordResetService(PasswordResetTokenRepository tokens, UserRepository users,
        RefreshSessionRepository sessions, PasswordEncoder encoder, PasswordResetDelivery delivery) {
        this.tokens = tokens;
        this.users = users;
        this.sessions = sessions;
        this.encoder = encoder;
        this.delivery = delivery;
    }

    @Transactional
    public void request(String email) {
        // Deliberate 503 when email delivery is unavailable, including unknown emails.
        // We must not claim to have delivered a reset message in a production build.
        if (!delivery.available()) throw new ApiProblem(HttpStatus.SERVICE_UNAVAILABLE,
            "RECOVERY_UNAVAILABLE", "Password recovery not configured");
        var user = users.findByEmail(SecurityStrings.normalizeEmail(email)).orElse(null);
        if (user == null) return; // Controller always returns a generic acceptance message.
        Instant now = Instant.now();
        for (var old : tokens.findByUserIdAndUsedAtIsNull(user.getId())) old.consume(now);
        String plaintext = SecurityStrings.randomToken();
        tokens.save(new PasswordResetToken(user.getId(), SecurityStrings.sha256(plaintext),
            now.plus(15, ChronoUnit.MINUTES)));
        delivery.deliver(user.getEmail(), plaintext);
    }

    @Transactional
    public void redeem(String plaintext, String newPassword) {
        Instant now = Instant.now();
        var token = tokens.lockByTokenHash(SecurityStrings.sha256(plaintext))
            .orElseThrow(PasswordResetService::invalidToken);
        if (!token.canRedeem(now)) throw invalidToken();
        var user = users.findById(token.getUserId()).orElseThrow(PasswordResetService::invalidToken);
        if (encoder.matches(newPassword, user.getPasswordHash()))
            throw new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,
                "PASSWORD_UNCHANGED", "Choose a different password");
        user.changePasswordHash(encoder.encode(newPassword));
        // All refresh-token families for this account are revoked.
        for (var session : sessions.findByUserIdAndRevokedAtIsNull(user.getId())) session.revoke(null);
        for (var issued : tokens.findByUserIdAndUsedAtIsNull(user.getId())) issued.consume(now);
    }

    private static ApiProblem invalidToken() {
        return new ApiProblem(HttpStatus.BAD_REQUEST,
            "INVALID_RESET_TOKEN", "Reset token is invalid or expired");
    }
}
