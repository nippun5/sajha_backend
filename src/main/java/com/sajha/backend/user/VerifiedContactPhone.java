package com.sajha.backend.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="verified_contact_phones")
public class VerifiedContactPhone {
    @Id @Column(length=36) private String id;
    @Column(name="user_id",nullable=false,length=36) private String userId;
    @Column(nullable=false,length=16,unique=true) private String phone;
    @Column(name="verified_at",nullable=false) private Instant verifiedAt;
    protected VerifiedContactPhone() {}
    public VerifiedContactPhone(String userId,String phone) {
        this.id=UUID.randomUUID().toString(); this.userId=userId;
        this.phone=phone; this.verifiedAt=Instant.now();
    }
    public String getPhone(){return phone;}
}
