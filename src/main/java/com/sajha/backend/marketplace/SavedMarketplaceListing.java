package com.sajha.backend.marketplace;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "saved_marketplace_listings")
public class SavedMarketplaceListing {
    @Id @Column(length = 36)
    private String id;
    @Column(name="user_id", length=36, nullable=false)
    private String userId;
    @Column(name="listing_id", length=36, nullable=false)
    private String listingId;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;
    protected SavedMarketplaceListing() {}
    public String getUserId() {return userId;}
    public String getListingId() {return listingId;}
}
