package com.sajha.backend.auth.social;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="social_identities")
public class SocialIdentity {
    @Id @Column(length=36,nullable=false) private String id;
    @Column(name="user_id",length=36,nullable=false) private String userId;
    @Column(length=16,nullable=false) private String provider;
    @Column(name="provider_subject",length=255,nullable=false) private String providerSubject;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected SocialIdentity() {}
    public SocialIdentity(String userId,String provider,String subject){
        id=UUID.randomUUID().toString();this.userId=userId;this.provider=provider;
        providerSubject=subject;createdAt=Instant.now();
    }
    public String getUserId(){return userId;}
}
