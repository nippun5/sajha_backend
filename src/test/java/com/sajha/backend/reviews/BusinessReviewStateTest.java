package com.sajha.backend.reviews;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BusinessReviewStateTest {
    @Test void reviewCanBeUpdatedDeletedAndModerated() {
        BusinessReview review = new BusinessReview("business", "reviewer", 5, "Helpful and friendly service");
        assertEquals("active", review.getStatus());
        assertEquals(5, review.getStars());
        review.setReview(4, "Updated after my second visit");
        assertEquals(4, review.getStars());
        review.delete();
        assertEquals("deleted", review.getStatus());
        review.moderateRemove();
        assertEquals("removed", review.getStatus());
    }

    @Test void reportCannotBeModeratedTwice() {
        BusinessReviewReport report = new BusinessReviewReport("review", "reporter", "Spam or advertising");
        assertEquals("open", report.getStatus());
        report.resolve("admin", "dismissed", "Review complies with published rules");
        assertEquals("dismissed", report.getStatus());
        assertThrows(IllegalStateException.class, () ->
            report.resolve("admin", "removed", "Cannot revisit closed report"));
    }
}
