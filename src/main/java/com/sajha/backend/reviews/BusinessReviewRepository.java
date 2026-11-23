package com.sajha.backend.reviews;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BusinessReviewRepository extends JpaRepository<BusinessReview,String> {
    List<BusinessReview> findByBusinessIdAndStatusOrderByUpdatedAtDesc(String businessId,String status);
    Optional<BusinessReview> findByBusinessIdAndAuthorId(String businessId,String authorId);
}
