package com.sajha.backend.recovery;

import com.sajha.backend.api.ApiProblem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Only usable with SPRING_PROFILES_ACTIVE=local AND explicit opt-in. Never deploy to Render. */
@Component
@Profile("local")
public class LocalConsolePasswordResetDelivery implements PasswordResetDelivery {
    private final boolean enabled;
    public LocalConsolePasswordResetDelivery(
        @Value("${SAJHA_LOCAL_PASSWORD_RESET_ENABLED:false}") boolean enabled) {
        this.enabled = enabled;
    }
    @Override public boolean available() { return enabled; }
    @Override public void deliver(String email, String token) {
        if (!enabled) throw new ApiProblem(HttpStatus.SERVICE_UNAVAILABLE,
            "RECOVERY_UNAVAILABLE", "Password recovery not configured");
        // This secret is intended for a developer's private terminal only.
        System.out.println("[SaJHa LOCAL PASSWORD RESET] email=" + email
            + " token=" + token + " (15 minutes; do not share)");
    }
}
