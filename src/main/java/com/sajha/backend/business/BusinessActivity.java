package com.sajha.backend.business;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="business_admin_activity")
public class BusinessActivity {
    @Id @Column(length=36) private String id;
    @Column(name="request_id",nullable=false,length=36) private String requestId;
    @Column(name="actor_id",nullable=false,length=36) private String actorId;
    @Column(name="action_type",nullable=false,length=30) private String actionType;
    @Column(nullable=false,length=800) private String detail;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected BusinessActivity(){}
    public BusinessActivity(String requestId,String actorId,String actionType,String detail){
        id=UUID.randomUUID().toString();this.requestId=requestId;this.actorId=actorId;
        this.actionType=actionType;this.detail=detail;createdAt=Instant.now();
    }
    public String getId(){return id;}public String getRequestId(){return requestId;}
    public String getActorId(){return actorId;}public String getActionType(){return actionType;}
    public String getDetail(){return detail;}public Instant getCreatedAt(){return createdAt;}
}
