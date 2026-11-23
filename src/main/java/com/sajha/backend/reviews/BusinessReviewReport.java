package com.sajha.backend.reviews;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="business_review_reports")
public class BusinessReviewReport {
    @Id @Column(length=36) private String id;
    @Column(name="review_id",length=36,nullable=false) private String reviewId;
    @Column(name="reporter_id",length=36,nullable=false) private String reporterId;
    @Column(length=80,nullable=false) private String reason;
    @Column(length=16,nullable=false) private String status;
    @Column(name="moderator_id",length=36) private String moderatorId;
    @Column(name="moderator_note",length=800) private String moderatorNote;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="moderated_at") private Instant moderatedAt;
    @Version @Column(name="row_version",nullable=false) private long rowVersion;

    protected BusinessReviewReport() {}
    public BusinessReviewReport(String reviewId,String reporterId,String reason){
        id=UUID.randomUUID().toString();this.reviewId=reviewId;
        this.reporterId=reporterId;this.reason=reason;
        status="open";createdAt=Instant.now();
    }
    public void resolve(String moderatorId,String decision,String note){
        if(!"open".equals(status))throw new IllegalStateException("Report already moderated");
        status=decision;this.moderatorId=moderatorId;this.moderatorNote=note;moderatedAt=Instant.now();
    }
    public String getId(){return id;}public String getReviewId(){return reviewId;}
    public String getReporterId(){return reporterId;}public String getReason(){return reason;}
    public String getStatus(){return status;}public String getModeratorNote(){return moderatorNote;}
    public Instant getCreatedAt(){return createdAt;}public Instant getModeratedAt(){return moderatedAt;}
}
