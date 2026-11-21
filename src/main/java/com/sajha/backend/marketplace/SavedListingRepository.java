package com.sajha.backend.marketplace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;

public interface SavedListingRepository extends JpaRepository<SavedMarketplaceListing, String> {
    @Modifying
    @Query(value="""
      INSERT IGNORE INTO saved_marketplace_listings (id, user_id, listing_id, created_at)
      VALUES (:id, :userId, :listingId, :createdAt)
      """,nativeQuery=true)
    int insertIfMissing(String id, String userId, String listingId, Instant createdAt);

    @Modifying
    @Query("DELETE FROM SavedMarketplaceListing s WHERE s.userId = :userId AND s.listingId = :listingId")
    int removeSaved(String userId, String listingId);
}
