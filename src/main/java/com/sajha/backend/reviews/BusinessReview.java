package com.sajha.backend.reviews;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="business_reviews")
public class BusinessReview {
    @Id @Column(length=36) private String id;
    @Column(name="business_id",length=36,nullable=false) private String businessId;
    @Column(name="author_id",length=36,nullable=false) private String authorId;
    @Column(nullable=false) private int stars;
    @Column(length=600,nullable=false) private String comment;
    @Column(length=16,nullable=false) private String status;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version @Column(name="row_version",nullable=false) private long rowVersion;

    protected BusinessReview() {}
    public BusinessReview(String businessId,String authorId,int stars,String comment){
        this.id=UUID.randomUUID().toString();this.businessId=businessId;this.authorId=authorId;
        this.createdAt=Instant.now();setReview(stars,comment);
    }
    public void setReview(int stars,String comment){
        this.stars=stars;this.comment=comment;this.status="active";this.updatedAt=Instant.now();
    }
    public void delete(){this.status="deleted";this.updatedAt=Instant.now();}
    public void moderateRemove(){this.status="removed";this.updatedAt=Instant.now();}
    public String getId(){return id;}public String getBusinessId(){return businessId;}
    public String getAuthorId(){return authorId;}public int getStars(){return stars;}
    public String getComment(){return comment;}public String getStatus(){return status;}
    public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}
}
