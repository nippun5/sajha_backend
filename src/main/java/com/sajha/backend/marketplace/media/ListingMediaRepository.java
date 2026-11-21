package com.sajha.backend.marketplace.media;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface ListingMediaRepository extends JpaRepository<ListingMedia,String> {
    List<ListingMedia> findByListingIdOrderBySortOrderAsc(String listingId);
    long countByOwnerIdAndListingIdIsNull(String ownerId);
    List<ListingMedia> findByOwnerIdAndListingIdIsNullAndCreatedAtBefore(String ownerId,Instant cutoff);
}
