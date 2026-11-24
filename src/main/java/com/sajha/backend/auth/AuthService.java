package com.sajha.backend.auth;

import com.sajha.backend.api.ApiProblem;
import com.sajha.backend.user.*;
import com.sajha.backend.util.SecurityStrings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final VerifiedContactPhoneRepository verifiedPhones;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwt;
    private final String issuer;
    private final long accessMinutes;
    private final long refreshDays;
    private final String dummyHash;

    public AuthService(UserRepository users,VerifiedContactPhoneRepository verifiedPhones,
                       RefreshSessionRepository sessions,PasswordEncoder encoder,JwtEncoder jwt,
                       @Value("${app.auth.issuer}") String issuer,
                       @Value("${app.auth.access-minutes:15}") long accessMinutes,
                       @Value("${app.auth.refresh-days:30}") long refreshDays){
        this.users=users;this.verifiedPhones=verifiedPhones;this.sessions=sessions;
        this.encoder=encoder;this.jwt=jwt;this.issuer=issuer;
        this.accessMinutes=accessMinutes;this.refreshDays=refreshDays;
        this.dummyHash=encoder.encode(SecurityStrings.randomToken());
    }
    @Transactional
    public AuthDtos.Tokens register(AuthDtos.Register input){
        String email=SecurityStrings.normalizeEmail(input.email());
        if(users.existsByEmail(email)) throw new ApiProblem(HttpStatus.CONFLICT,"EMAIL_TAKEN","Email already registered");
        AppUser user=users.save(new AppUser(input.name().trim(),email,encoder.encode(input.password()),input.phone()));
        return newSession(user);
    }
    @Transactional
    public AuthDtos.Tokens login(AuthDtos.Login input){
        var user=users.findByEmail(SecurityStrings.normalizeEmail(input.email())).orElse(null);
        // A dummy BCrypt hash keeps unknown-user timing closer to a normal password check.
        boolean passwordValid=encoder.matches(input.password(),user==null?dummyHash:user.getPasswordHash());
        if(user==null || !passwordValid)
            throw new ApiProblem(HttpStatus.UNAUTHORIZED,"BAD_CREDENTIALS","Invalid email or password");
        return newSession(user);
    }
    /** Server-verified social identities receive the same tokens and roles as password login. */
    @Transactional
    public AuthDtos.Tokens issueSocialSession(AppUser user){return newSession(user);}
    private AuthDtos.Tokens newSession(AppUser user){
        String token=SecurityStrings.randomToken();
        sessions.save(new RefreshSession(user.getId(),UUID.randomUUID().toString(),
            SecurityStrings.sha256(token),Instant.now().plus(refreshDays,ChronoUnit.DAYS)));
        return tokens(user,token);
    }
    private AuthDtos.Tokens tokens(AppUser user,String refreshToken){
        Instant now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer(issuer).subject(user.getId()).issuedAt(now)
            .expiresAt(now.plus(accessMinutes,ChronoUnit.MINUTES))
            .claim("roles",new ArrayList<>(user.getRoles())).claim("av",user.getAuthVersion()).build();
        var header=JwsHeader.with(MacAlgorithm.HS256).build();
        String access=jwt.encode(JwtEncoderParameters.from(header,claims)).getTokenValue();
        return new AuthDtos.Tokens(access,refreshToken,"Bearer",accessMinutes*60,account(user));
    }
    @Transactional(readOnly=true)
    public AuthDtos.Account account(String id){
        AppUser user=users.findById(id).orElseThrow(()->new ApiProblem(HttpStatus.UNAUTHORIZED,
            "UNAUTHENTICATED","Account not found"));
        return account(user);
    }
    private AuthDtos.Account account(AppUser user){
        return new AuthDtos.Account(user.getId(),user.getName(),user.getEmail(),user.getPhone(),
            user.isPhoneVerified(),verifiedPhones.findByUserId(user.getId()).stream()
            .map(VerifiedContactPhone::getPhone).toList(),user.getRoles());
    }
    @Transactional(noRollbackFor=TokenReuseDetected.class)
    public AuthDtos.Tokens refresh(AuthDtos.Refresh input){
        RefreshSession current=sessions.lockedByTokenHash(SecurityStrings.sha256(input.refreshToken()))
            .orElseThrow(()->new ApiProblem(HttpStatus.UNAUTHORIZED,"INVALID_REFRESH","Refresh token invalid"));
        if(current.getRevokedAt()!=null){
            sessions.findByFamilyIdAndRevokedAtIsNull(current.getFamilyId())
                .forEach(s->s.revoke(null));
            throw new TokenReuseDetected();
        }
        if(!current.isActive()) throw new ApiProblem(HttpStatus.UNAUTHORIZED,"EXPIRED_REFRESH","Refresh token expired");
        AppUser user=users.findById(current.getUserId()).orElseThrow(()->new ApiProblem(HttpStatus.UNAUTHORIZED,
            "INVALID_REFRESH","Refresh token invalid"));
        String newToken=SecurityStrings.randomToken();
        RefreshSession next=sessions.save(new RefreshSession(user.getId(),current.getFamilyId(),
            SecurityStrings.sha256(newToken),Instant.now().plus(refreshDays,ChronoUnit.DAYS)));
        current.revoke(next.getId());
        return tokens(user,newToken);
    }
    @Transactional
    public void logout(String userId,AuthDtos.Refresh input){
        sessions.lockedByTokenHash(SecurityStrings.sha256(input.refreshToken()))
            .filter(s->s.getUserId().equals(userId)).ifPresent(s->{if(s.getRevokedAt()==null)s.revoke(null);});
    }
    public static class TokenReuseDetected extends ApiProblem {
        public TokenReuseDetected(){super(HttpStatus.UNAUTHORIZED,"REFRESH_REUSED","Refresh session revoked; sign in again");}
    }
}
