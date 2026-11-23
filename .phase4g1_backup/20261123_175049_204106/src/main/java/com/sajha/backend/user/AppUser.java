package com.sajha.backend.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name="app_users")
public class AppUser {
    @Id @Column(length=36,nullable=false) private String id;
    @Column(nullable=false,length=100) private String name;
    @Column(nullable=false,length=254,unique=true) private String email;
    @Column(name="password_hash",nullable=false,length=255) private String passwordHash;
    @Column(nullable=false,length=16) private String phone;
    @Column(name="phone_verified",nullable=false) private boolean phoneVerified;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="user_roles",joinColumns=@JoinColumn(name="user_id"))
    @Column(name="role",nullable=false,length=30)
    private Set<String> roles=new HashSet<>();

    protected AppUser() {}
    public AppUser(String name,String email,String passwordHash,String phone) {
        this.id=UUID.randomUUID().toString(); this.name=name; this.email=email;
        this.passwordHash=passwordHash; this.phone=phone; this.phoneVerified=false;
        this.createdAt=Instant.now(); this.updatedAt=this.createdAt;
        this.roles.add("USER");
    }
    public String getId(){return id;}
    public String getName(){return name;}
    public String getEmail(){return email;}
    public String getPasswordHash(){return passwordHash;}
    public String getPhone(){return phone;}
    public boolean isPhoneVerified(){return phoneVerified;}
    public Set<String> getRoles(){return roles;}
    public void verifyAccountPhone(){this.phoneVerified=true;this.updatedAt=Instant.now();}
}
