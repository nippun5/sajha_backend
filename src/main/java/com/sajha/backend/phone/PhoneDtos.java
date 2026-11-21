package com.sajha.backend.phone;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class PhoneDtos {
    private PhoneDtos(){}
    public record Start(@NotBlank @Pattern(regexp="^\\+[1-9][0-9]{6,14}$") String phone){}
    public record Started(String challengeId,Instant expiresAt){}
    public record Verify(@NotBlank @Size(min=36,max=36) String challengeId,
                         @NotBlank @Size(min=4,max=10) String code){}
    public record Verified(boolean phoneVerified,String phone){}
}
