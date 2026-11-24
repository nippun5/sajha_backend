package com.sajha.backend.auth.social;

import com.sajha.backend.api.ApiProblem;
import com.sajha.backend.auth.AuthDtos;
import com.sajha.backend.auth.AuthService;
import com.sajha.backend.user.AppUser;
import com.sajha.backend.user.UserRepository;
import com.sajha.backend.util.SecurityStrings;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;

@Service
public class SocialAuthService {
    private final SocialProviderVerifier verifier;
    private final SocialIdentityRepository links;
    private final SocialSignupRepository pending;
    private final SocialNonceService nonces;
    private final UserRepository users;
    private final AuthService auth;
    private final PasswordEncoder encoder;

    public SocialAuthService(SocialProviderVerifier verifier,SocialIdentityRepository links,
        SocialSignupRepository pending,SocialNonceService nonces,UserRepository users,AuthService auth,PasswordEncoder encoder){
        this.verifier=verifier;this.links=links;this.pending=pending;this.nonces=nonces;
        this.users=users;this.auth=auth;this.encoder=encoder;
    }
    /** 200 Tokens for linked identities; 202 Pending for verified new accounts.
     * Facebook new accounts require a separate verified email onboarding path (not yet enabled).
     */
    @Transactional
    public Object start(String provider,String credential,String nonce){
        var proof=verifier.verify(provider,credential,nonce);
        if(proof.provider().equals("apple")) nonces.consume(nonce);
        var linked=links.findByProviderAndProviderSubject(proof.provider(),proof.subject());
        if(linked.isPresent()){
            var owner=users.findById(linked.get().getUserId()).orElseThrow(()->
                new ApiProblem(HttpStatus.UNAUTHORIZED,"INVALID_IDENTITY","Linked account missing"));
            return auth.issueSocialSession(owner);
        }
        if(!proof.emailVerified() || proof.email()==null || proof.email().isBlank()){
            throw new ApiProblem(HttpStatus.CONFLICT,"EMAIL_VERIFICATION_REQUIRED",
                "This provider needs an existing account. Sign in with email first, then link it.");
        }
        var email=SecurityStrings.normalizeEmail(proof.email());
        if(users.existsByEmail(email)) throw new ApiProblem(HttpStatus.CONFLICT,"SOCIAL_LINK_REQUIRED",
            "An account with this email exists. Sign in using its existing method and link this provider.");
        String plain=SecurityStrings.randomToken();
        pending.save(new SocialSignupChallenge(proof.provider(),proof.subject(),email,
            proof.name(),SecurityStrings.sha256(plain)));
        return new SocialDtos.Pending("PHONE_REQUIRED",plain,600,proof.name());
    }
    @Transactional
    public AuthDtos.Tokens complete(SocialDtos.Complete input){
        var record=pending.findByTokenHash(SecurityStrings.sha256(input.completionToken()))
            .orElseThrow(SocialAuthService::invalidSignup);
        if(!record.valid(Instant.now())) throw invalidSignup();
        // New account remains phone UNVERIFIED. Production verification is separate.
        if(users.existsByEmail(record.getEmail())) throw new ApiProblem(HttpStatus.CONFLICT,
            "EMAIL_TAKEN","An account with this email already exists");
        if(links.findByProviderAndProviderSubject(record.getProvider(),record.getProviderSubject()).isPresent())
            throw invalidSignup();
        String randomPassword=SecurityStrings.randomToken();
        AppUser created=users.save(new AppUser(input.name().trim(),record.getEmail(),
            encoder.encode(randomPassword),input.phone()));
        links.save(new SocialIdentity(created.getId(),record.getProvider(),record.getProviderSubject()));
        record.consume(Instant.now());
        return auth.issueSocialSession(created);
    }
    @Transactional
    public void link(String userId,String provider,String credential,String nonce){
        var proof=verifier.verify(provider,credential,nonce);
        if(proof.provider().equals("apple")) nonces.consume(nonce);
        var user=users.findById(userId).orElseThrow(()->new ApiProblem(HttpStatus.UNAUTHORIZED,
            "ACCOUNT_NOT_FOUND","Please sign in again"));
        if(links.findByProviderAndProviderSubject(proof.provider(),proof.subject()).isPresent())
            throw new ApiProblem(HttpStatus.CONFLICT,"IDENTITY_ALREADY_LINKED","This identity is already linked");
        if(links.existsByUserIdAndProvider(user.getId(),proof.provider()))
            throw new ApiProblem(HttpStatus.CONFLICT,"PROVIDER_ALREADY_LINKED","An account is already linked for this provider");
        // Crucially, linking is authorized by the CURRENT authenticated user, not email matching.
        links.save(new SocialIdentity(user.getId(),proof.provider(),proof.subject()));
    }
    private static ApiProblem invalidSignup(){return new ApiProblem(HttpStatus.BAD_REQUEST,
        "INVALID_SOCIAL_SIGNUP","Social signup expired. Start again.");}
}
