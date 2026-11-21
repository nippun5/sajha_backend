package com.sajha.backend.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="refresh_sessions")
public class RefreshSession {
    @Id @Column(length=36) private String id;
    @Column(name="user_id",nullable=false,length=36) private String userId;
    @Column(name="family_id",nullable=false,length=36) private String familyId;
    @Column(name="token_hash",nullable=false,length=64,unique=true) private String tokenHash;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="revoked_at") private Instant revokedAt;
    @Column(name="replaced_by",length=36) private String replacedBy;
    protected RefreshSession() {}
    public RefreshSession(String userId,String familyId,String tokenHash,Instant expiresAt) {
        this.id=UUID.randomUUID().toString(); this.userId=userId;this.familyId=familyId;
        this.tokenHash=tokenHash;this.expiresAt=expiresAt;this.createdAt=Instant.now();
    }
    public String getId(){return id;}
    public String getUserId(){return userId;}
    public String getFamilyId(){return familyId;}
    public Instant getExpiresAt(){return expiresAt;}
    public Instant getRevokedAt(){return revokedAt;}
    public boolean isActive(){return revokedAt==null && expiresAt.isAfter(Instant.now());}
    public void revoke(String replacementId){revokedAt=Instant.now();replacedBy=replacementId;}
}
