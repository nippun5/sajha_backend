package com.sajha.backend.recovery;

/** Separate delivery from token generation. Never return recovery secrets in an HTTP response. */
public interface PasswordResetDelivery {
    boolean available();
    void deliver(String email, String resetToken);
}
