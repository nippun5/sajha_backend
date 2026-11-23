package com.sajha.backend.recovery;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id @Column(length = 36, nullable = false)
    private String id;
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;
    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;

    protected PasswordResetToken() {}
    public PasswordResetToken(String userId, String tokenHash, Instant expiresAt) {
        id = UUID.randomUUID().toString();
        this.userId = userId;
        this.tokenHash = tokenHash;
        createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public boolean canRedeem(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
    public void consume(Instant when) { usedAt = when; }
}
