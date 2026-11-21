package com.sajha.backend.marketplace.media;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_media")
public class ListingMedia {
    @Id @Column(length=36, nullable=false)
    private String id;
    @Column(name="owner_id", length=36, nullable=false)
    private String ownerId;
    @Column(name="listing_id", length=36)
    private String listingId;
    @Column(name="storage_key", length=80, nullable=false, unique=true)
    private String storageKey;
    @Column(name="mime_type", length=24, nullable=false)
    private String mimeType;
    @Column(name="byte_size", nullable=false)
    private long byteSize;
    @Column(name="sort_order", nullable=false)
    private int sortOrder;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    protected ListingMedia() {}
    public ListingMedia(String ownerId, String key, String mime, long bytes) {
        this.id=UUID.randomUUID().toString();
        this.ownerId=ownerId;
        this.storageKey=key;
        this.mimeType=mime;
        this.byteSize=bytes;
        this.createdAt=Instant.now();
    }
    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getListingId() { return listingId; }
    public String getStorageKey() { return storageKey; }
    public String getMimeType() { return mimeType; }
    public long getByteSize() { return byteSize; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void attach(String listingId, int position) {this.listingId=listingId;this.sortOrder=position;}
    public void detach() {this.listingId=null;this.sortOrder=0;}
}
