package com.sajha.backend.security;

import com.sajha.backend.auth.RefreshSessionRepository;
import com.sajha.backend.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/sessions")
public class SessionRevocationController {
    private final UserRepository users;
    private final RefreshSessionRepository sessions;
    public SessionRevocationController(UserRepository users,RefreshSessionRepository sessions){
        this.users=users;this.sessions=sessions;
    }
    @PostMapping("/revoke-all")
    @Transactional
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal Jwt jwt){
        var user=users.findById(jwt.getSubject()).orElseThrow();
        user.revokeAccessTokens();
        sessions.findByUserIdAndRevokedAtIsNull(user.getId()).forEach(s->s.revoke(null));
        return ResponseEntity.noContent().build();
    }
}
