package com.sajha.backend.recovery;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PasswordResetDtos {
    private PasswordResetDtos() {}
    public record Forgot(@NotBlank @Email String email) {}
    public record Redeem(@NotBlank String token,
                         @NotBlank @Size(min = 10, max = 128) String newPassword) {}
    public record Accepted(String message) {}
}
