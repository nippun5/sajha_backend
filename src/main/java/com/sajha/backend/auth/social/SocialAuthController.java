package com.sajha.backend.auth.social;

import com.sajha.backend.auth.AuthDtos;
import com.sajha.backend.security.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController
public class SocialAuthController {
    private final SocialAuthService service;
    private final RateLimitService limiter;
    public SocialAuthController(SocialAuthService service,RateLimitService limiter,SocialNonceService nonces){this.service=service;this.limiter=limiter;this.nonces=nonces;}
    private final SocialNonceService nonces;
    @PostMapping("/api/v1/auth/social/apple/challenge")
    public SocialNonceService.Challenge appleChallenge(HttpServletRequest request){
        limiter.check("apple-nonce:"+request.getRemoteAddr(),15,Duration.ofMinutes(15));
        return nonces.issue();
    }
    @PostMapping("/api/v1/auth/social/{provider}")
    public ResponseEntity<Object> begin(@PathVariable String provider,
        @Valid @RequestBody SocialDtos.SignIn input,HttpServletRequest request){
        limiter.check("social-begin:"+request.getRemoteAddr(),12,Duration.ofMinutes(15));
        Object outcome=service.start(provider,input.credential(),input.nonce());
        return ResponseEntity.status(outcome instanceof SocialDtos.Pending ? HttpStatus.ACCEPTED : HttpStatus.OK).body(outcome);
    }
    @PostMapping("/api/v1/auth/social/complete")
    public ResponseEntity<AuthDtos.Tokens> complete(@Valid @RequestBody SocialDtos.Complete input,
        HttpServletRequest request){
        limiter.check("social-complete:"+request.getRemoteAddr(),8,Duration.ofMinutes(15));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.complete(input));
    }
    @PostMapping("/api/v1/me/social/{provider}/link")
    public ResponseEntity<Void> link(@AuthenticationPrincipal Jwt user,@PathVariable String provider,
        @Valid @RequestBody SocialDtos.Link input,HttpServletRequest request){
        limiter.check("social-link:"+user.getSubject(),8,Duration.ofMinutes(15));
        service.link(user.getSubject(),provider,input.credential(),input.nonce());
        return ResponseEntity.noContent().build();
    }
}
