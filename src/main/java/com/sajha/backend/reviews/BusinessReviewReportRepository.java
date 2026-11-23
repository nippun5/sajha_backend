package com.sajha.backend.reviews;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BusinessReviewReportRepository extends JpaRepository<BusinessReviewReport,String> {
    List<BusinessReviewReport> findAllByOrderByCreatedAtDesc();
    List<BusinessReviewReport> findByReviewIdAndStatus(String reviewId,String status);
    Optional<BusinessReviewReport> findByReviewIdAndReporterId(String reviewId,String reporterId);
}
