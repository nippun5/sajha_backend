package com.sajha.backend.recovery;

import com.sajha.backend.auth.RefreshSession;
import com.sajha.backend.auth.RefreshSessionRepository;
import com.sajha.backend.user.AppUser;
import com.sajha.backend.user.UserRepository;
import com.sajha.backend.util.SecurityStrings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    @Mock PasswordResetTokenRepository tokens;
    @Mock UserRepository users;
    @Mock RefreshSessionRepository sessions;
    @Mock PasswordEncoder encoder;
    @Mock PasswordResetDelivery delivery;
    @InjectMocks PasswordResetService service;

    @Test void unknownEmailDoesNotCreateToken() {
        when(delivery.available()).thenReturn(true);
        when(users.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        service.request("missing@example.com");
        verifyNoInteractions(tokens);
        verify(delivery, never()).deliver(anyString(), anyString());
    }

    @Test void newResetStoresHashNotPlaintext() {
        var user = new AppUser("Tester", "tester@example.com", "hash", "+13145550123");
        when(delivery.available()).thenReturn(true);
        when(users.findByEmail("tester@example.com")).thenReturn(Optional.of(user));
        when(tokens.findByUserIdAndUsedAtIsNull(user.getId())).thenReturn(List.of());
        service.request("tester@example.com");
        var saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        var sent = ArgumentCaptor.forClass(String.class);
        verify(tokens).save(saved.capture());
        verify(delivery).deliver(eq("tester@example.com"), sent.capture());
        assertEquals(SecurityStrings.sha256(sent.getValue()), saved.getValue().getTokenHash());
        assertNotEquals(sent.getValue(), saved.getValue().getTokenHash());
    }

    @Test void successfulResetRevokesRefreshSessionsAndConsumesToken() {
        var user = new AppUser("Tester", "tester@example.com", "oldHash", "+13145550123");
        String plain = SecurityStrings.randomToken();
        var token = new PasswordResetToken(user.getId(), SecurityStrings.sha256(plain),
            Instant.now().plusSeconds(900));
        var session = new RefreshSession(user.getId(), "family", "hash", Instant.now().plusSeconds(900));
        when(tokens.lockByTokenHash(SecurityStrings.sha256(plain))).thenReturn(Optional.of(token));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(encoder.matches("brand-new-password", "oldHash")).thenReturn(false);
        when(encoder.encode("brand-new-password")).thenReturn("newHash");
        when(sessions.findByUserIdAndRevokedAtIsNull(user.getId())).thenReturn(List.of(session));
        when(tokens.findByUserIdAndUsedAtIsNull(user.getId())).thenReturn(List.of(token));
        service.redeem(plain, "brand-new-password");
        assertEquals("newHash", user.getPasswordHash());
        assertNotNull(session.getRevokedAt());
        assertNotNull(token.getUsedAt());
    }
}
