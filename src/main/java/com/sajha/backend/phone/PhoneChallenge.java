package com.sajha.backend.phone;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="phone_challenges")
public class PhoneChallenge {
    @Id @Column(length=36) private String id;
    @Column(name="user_id",nullable=false,length=36) private String userId;
    @Column(nullable=false,length=16) private String phone;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(nullable=false) private int attempts;
    @Column(name="completed_at") private Instant completedAt;
    protected PhoneChallenge() {}
    public PhoneChallenge(String userId,String phone){
        this.id=UUID.randomUUID().toString();this.userId=userId;this.phone=phone;
        this.createdAt=Instant.now();this.expiresAt=this.createdAt.plusSeconds(600);
    }
    public String getId(){return id;}
    public String getUserId(){return userId;}
    public String getPhone(){return phone;}
    public Instant getCreatedAt(){return createdAt;}
    public Instant getExpiresAt(){return expiresAt;}
    public int getAttempts(){return attempts;}
    public boolean isCompleted(){return completedAt!=null;}
    public boolean isExpired(){return Instant.now().isAfter(expiresAt);}
    public void attempt(){attempts++;}
    public void complete(){completedAt=Instant.now();}
}
