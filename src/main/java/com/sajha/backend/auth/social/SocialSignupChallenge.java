package com.sajha.backend.auth.social;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="social_signup_challenges")
public class SocialSignupChallenge {
    @Id @Column(length=36,nullable=false) private String id;
    @Column(length=16,nullable=false) private String provider;
    @Column(name="provider_subject",length=255,nullable=false) private String providerSubject;
    @Column(length=254,nullable=false) private String email;
    @Column(name="display_name",length=100) private String displayName;
    @Column(name="token_hash",length=64,nullable=false,unique=true) private String tokenHash;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="consumed_at") private Instant consumedAt;
    protected SocialSignupChallenge() {}
    public SocialSignupChallenge(String provider,String subject,String email,String displayName,String hash){
        id=UUID.randomUUID().toString();this.provider=provider;providerSubject=subject;
        this.email=email;this.displayName=displayName;tokenHash=hash;
        expiresAt=Instant.now().plusSeconds(600);
    }
    public String getProvider(){return provider;}
    public String getProviderSubject(){return providerSubject;}
    public String getEmail(){return email;}
    public String getDisplayName(){return displayName;}
    public boolean valid(Instant now){return consumedAt==null && expiresAt.isAfter(now);}
    public void consume(Instant now){consumedAt=now;}
}
