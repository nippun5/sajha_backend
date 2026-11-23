package com.sajha.backend.recovery;

import com.sajha.backend.api.ApiProblem;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Production fails closed until a proper private email delivery provider is installed. */
@Component
@Profile("!local")
public class DisabledPasswordResetDelivery implements PasswordResetDelivery {
    @Override public boolean available() { return false; }
    @Override public void deliver(String email, String token) {
        throw new ApiProblem(HttpStatus.SERVICE_UNAVAILABLE,
            "RECOVERY_UNAVAILABLE", "Password recovery not configured");
    }
}
