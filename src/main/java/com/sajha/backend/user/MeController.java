package com.sajha.backend.user;

import com.sajha.backend.auth.AuthDtos;
import com.sajha.backend.auth.AuthService;
import com.sajha.backend.phone.PhoneDtos;
import com.sajha.backend.phone.PhoneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/me")
public class MeController {
    private final AuthService auth;
    private final PhoneService phones;
    public MeController(AuthService auth,PhoneService phones){this.auth=auth;this.phones=phones;}
    @GetMapping public AuthDtos.Account me(@AuthenticationPrincipal Jwt jwt){
        return auth.account(jwt.getSubject());
    }
    @PostMapping("/phone/challenges")
    public ResponseEntity<PhoneDtos.Started> start(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody PhoneDtos.Start request){
        return ResponseEntity.status(HttpStatus.CREATED).body(phones.start(jwt.getSubject(),request));
    }
    @PostMapping("/phone/verify")
    public PhoneDtos.Verified verify(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody PhoneDtos.Verify request){
        return phones.verify(jwt.getSubject(),request);
    }
}
