package com.sajha.backend.auth;

import com.sajha.backend.security.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final RateLimitService limiter;
    public AuthController(AuthService auth,RateLimitService limiter){this.auth=auth;this.limiter=limiter;}
    @PostMapping("/register")
    public ResponseEntity<AuthDtos.Tokens> register(@Valid @RequestBody AuthDtos.Register input,HttpServletRequest req){
        limiter.check("register:"+req.getRemoteAddr(),5,Duration.ofHours(1));
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(input));
    }
    @PostMapping("/login")
    public AuthDtos.Tokens login(@Valid @RequestBody AuthDtos.Login input,HttpServletRequest req){
        limiter.check("login:"+req.getRemoteAddr()+":"+input.email().trim().toLowerCase(java.util.Locale.ROOT),
            10,Duration.ofMinutes(15));
        return auth.login(input);
    }
    @PostMapping("/refresh")
    public AuthDtos.Tokens refresh(@Valid @RequestBody AuthDtos.Refresh input,HttpServletRequest req){
        limiter.check("refresh:"+req.getRemoteAddr(),60,Duration.ofMinutes(10));
        return auth.refresh(input);
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt user,@Valid @RequestBody AuthDtos.Refresh input){
        auth.logout(user.getSubject(),input);return ResponseEntity.noContent().build();
    }
}
