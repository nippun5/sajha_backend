package com.sajha.backend.auth.social;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="social_login_nonces")
public class SocialLoginNonce {
    @Id @Column(length=36,nullable=false) private String id;
    @Column(name="nonce_hash",length=64,nullable=false,unique=true) private String nonceHash;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="consumed_at") private Instant consumedAt;
    protected SocialLoginNonce() {}
    public SocialLoginNonce(String hash){id=UUID.randomUUID().toString();nonceHash=hash;expiresAt=Instant.now().plusSeconds(300);}
    public boolean valid(){return consumedAt==null && expiresAt.isAfter(Instant.now());}
    public void consume(){consumedAt=Instant.now();}
}
