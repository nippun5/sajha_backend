package com.sajha.backend.auth;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Set;

public final class AuthDtos {
    private AuthDtos(){}
    public record Register(
        @NotBlank @Size(min=2,max=100) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min=10,max=128) String password,
        @NotBlank @Pattern(regexp="^\\+[1-9][0-9]{6,14}$") String phone
    ){}
    public record Login(@NotBlank @Email String email,@NotBlank String password){}
    public record Refresh(@NotBlank @Size(min=32,max=256) String refreshToken){}
    public record Account(String id,String name,String email,String phone,boolean phoneVerified,
                          List<String> verifiedContactPhones,Set<String> roles){}
    public record Tokens(String accessToken,String refreshToken,String tokenType,long expiresIn,Account user){}
}
