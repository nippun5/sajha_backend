package com.sajha.backend.marketplace;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_listings")
public class MarketplaceListing {
    @Id @Column(length = 36, nullable = false)
    private String id;
    @Column(name = "owner_id", length = 36, nullable = false)
    private String ownerId;
    @Column(name = "owner_display_name", length = 100, nullable = false)
    private String ownerDisplayName;
    @Column(length = 16, nullable = false)
    private String kind;
    @Column(length = 120, nullable = false)
    private String title;
    @Column(columnDefinition = "text", nullable = false)
    private String description;
    @Column(name = "contact_phone", length = 16, nullable = false)
    private String contactPhone;
    @Column(length = 180, nullable = false)
    private String location;
    @Column(name = "details_json", columnDefinition = "longtext", nullable = false)
    private String detailsJson;
    @Column(length = 16, nullable = false)
    private String status;
    @Column(name = "idempotency_key", length = 80)
    private String idempotencyKey;
    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version @Column(nullable = false)
    private long version;

    protected MarketplaceListing() {}

    public MarketplaceListing(String ownerId, String ownerDisplayName, String kind,
                              String title, String description, String contactPhone,
                              String location, String detailsJson, String idempotencyKey,
                              String requestFingerprint) {
        this.id = UUID.randomUUID().toString();
        this.ownerId = ownerId;
        this.ownerDisplayName = ownerDisplayName;
        this.kind = kind;
        this.title = title;
        this.description = description;
        this.contactPhone = contactPhone;
        this.location = location;
        this.detailsJson = detailsJson;
        this.status = "active";
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }
    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getOwnerDisplayName() { return ownerDisplayName; }
    public String getKind() { return kind; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getContactPhone() { return contactPhone; }
    public String getLocation() { return location; }
    public String getDetailsJson() { return detailsJson; }
    public String getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version + 1; } // API is 1-based, JPA version is 0-based.

    public void update(String title, String description, String contactPhone,
                       String location, String detailsJson) {
        this.title = title;
        this.description = description;
        this.contactPhone = contactPhone;
        this.location = location;
        this.detailsJson = detailsJson;
        this.updatedAt = Instant.now();
    }
    public void delete() {
        status = "deleted";
        updatedAt = Instant.now();
    }
}
