package com.sajha.backend.recovery;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenTest {
    @Test void expiresAndCannotBeReused() {
        Instant now = Instant.now();
        var valid = new PasswordResetToken("test", "hash1", now.plusSeconds(900));
        assertTrue(valid.canRedeem(now));
        valid.consume(now);
        assertFalse(valid.canRedeem(now));
        var expired = new PasswordResetToken("test", "hash2", now.minusSeconds(1));
        assertFalse(expired.canRedeem(now));
    }
}
