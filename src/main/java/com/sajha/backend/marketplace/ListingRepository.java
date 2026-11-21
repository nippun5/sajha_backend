package com.sajha.backend.marketplace;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface ListingRepository extends JpaRepository<MarketplaceListing, String> {
    Optional<MarketplaceListing> findByOwnerIdAndIdempotencyKey(String ownerId, String idempotencyKey);
    Page<MarketplaceListing> findByOwnerIdAndStatusNot(String ownerId, String status, Pageable pageable);

    @Query("""
      SELECT l FROM MarketplaceListing l
      WHERE l.status = 'active'
        AND (:kind IS NULL OR l.kind = :kind)
        AND (:term IS NULL OR LOWER(l.title) LIKE CONCAT('%', :term, '%')
             OR LOWER(l.description) LIKE CONCAT('%', :term, '%')
             OR LOWER(l.location) LIKE CONCAT('%', :term, '%'))
      """)
    Page<MarketplaceListing> findPublic(String kind, String term, Pageable pageable);

    @Query("""
      SELECT l FROM MarketplaceListing l WHERE l.status = 'active'
      AND l.id IN (SELECT s.listingId FROM SavedMarketplaceListing s WHERE s.userId = :userId)
      """)
    Page<MarketplaceListing> findSaved(String userId, Pageable pageable);
}
