package com.sajha.backend.auth.social;

import jakarta.validation.constraints.*;

public final class SocialDtos {
    private SocialDtos() {}
    public record SignIn(@NotBlank String credential,String nonce) {}
    public record Pending(String status,String completionToken,long expiresIn,String suggestedName) {}
    public record Complete(@NotBlank String completionToken,
        @NotBlank @Size(min=2,max=100) String name,
        @NotBlank @Pattern(regexp="^\\+[1-9][0-9]{6,14}$") String phone) {}
    public record Link(@NotBlank String credential,String nonce) {}
}
