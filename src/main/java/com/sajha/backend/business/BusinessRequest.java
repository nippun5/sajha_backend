package com.sajha.backend.business;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="business_requests")
public class BusinessRequest {
    @Id @Column(length=36,nullable=false) private String id;
    @Column(name="owner_id",length=36,nullable=false) private String ownerId;
    @Column(name="business_name",length=100,nullable=false) private String businessName;
    @Column(length=60,nullable=false) private String category;
    @Column(name="contact_phone",length=16,nullable=false) private String phone;
    @Column(length=300,nullable=false) private String website;
    @Column(columnDefinition="text",nullable=false) private String description;
    @Column(name="online_only",nullable=false) private boolean onlineOnly;
    @Column(length=250,nullable=false) private String address;
    private Double latitude;
    private Double longitude;
    @Column(name="place_id",length=200) private String placeId;
    @Column(length=100) private String city;
    @Column(length=100) private String state;
    @Column(name="country_code",length=8) private String countryCode;
    @Column(name="opening_hours_json",columnDefinition="longtext",nullable=false) private String openingHoursJson;
    @Column(name="opening_hours_structured_json",columnDefinition="longtext",nullable=false) private String openingHoursStructuredJson;
    @Column(name="banner_storage_key",length=50,nullable=false) private String bannerStorageKey;
    @Column(name="banner_mime",length=30,nullable=false) private String bannerMime;
    @Column(length=16,nullable=false) private String status;
    @Column(length=16,nullable=false) private String visibility;
    @Column(name="decline_reason",length=800) private String declineReason;
    @Column(name="suspension_reason",length=800) private String suspensionReason;
    @Column(name="reviewed_at") private Instant reviewedAt;
    @Column(name="reviewed_by",length=36) private String reviewedBy;
    @Column(name="payment_confirmed",nullable=false) private boolean paymentConfirmed;
    @Column(name="payment_verified_at") private Instant paymentVerifiedAt;
    @Column(name="payment_verified_by",length=36) private String paymentVerifiedBy;
    @Column(name="payment_reference",length=100) private String paymentReference;
    @Column(name="payment_note",length=400) private String paymentNote;
    @Column(nullable=false) private int revision;
    @Column(name="submitted_at",nullable=false) private Instant submittedAt;
    @Column(name="last_submitted_at",nullable=false) private Instant lastSubmittedAt;
    @Version @Column(name="row_version",nullable=false) private long rowVersion;

    protected BusinessRequest() {}
    public BusinessRequest(String ownerId, BusinessDtos.Submission form, String hours, String structured,
                           String storageKey, String mime) {
        id=UUID.randomUUID().toString(); this.ownerId=ownerId;
        assign(form,hours,structured,storageKey,mime);
        status="pending";visibility="active";revision=1;
        submittedAt=Instant.now();lastSubmittedAt=submittedAt;
    }
    public void resubmit(BusinessDtos.Submission form,String hours,String structured,String key,String mime) {
        if (!"declined".equals(status)) throw new IllegalStateException("Only declined requests can be resubmitted");
        assign(form,hours,structured,key,mime);
        status="pending";visibility="active";declineReason=null;suspensionReason=null;
        reviewedAt=null;reviewedBy=null;
        paymentConfirmed=false;paymentVerifiedAt=null;paymentVerifiedBy=null;
        paymentReference=null;paymentNote=null;
        revision++;lastSubmittedAt=Instant.now();
    }
    private void assign(BusinessDtos.Submission f,String hours,String structured,String key,String mime) {
        businessName=f.name().trim();category=f.category().trim();phone=f.phone().trim();
        website=f.website()==null?"":f.website().trim();description=f.description().trim();
        onlineOnly=f.onlineOnly(); address=onlineOnly?"":f.address().trim();
        latitude=onlineOnly?null:f.latitude();longitude=onlineOnly?null:f.longitude();
        placeId=onlineOnly?null:f.placeId();city=onlineOnly?null:f.city();state=onlineOnly?null:f.state();
        countryCode=onlineOnly?null:f.countryCode();openingHoursJson=hours;
        openingHoursStructuredJson=structured;bannerStorageKey=key;bannerMime=mime;
    }
    public void confirmPayment(String actor,String reference,String note) {
        requirePending();paymentConfirmed=true;paymentVerifiedAt=Instant.now();paymentVerifiedBy=actor;
        paymentReference=reference;paymentNote=note;
    }
    public void clearPayment() {
        requirePending();paymentConfirmed=false;paymentVerifiedAt=null;paymentVerifiedBy=null;
        paymentReference=null;paymentNote=null;
    }
    public void approve(String actor) {
        requirePending();if (!paymentConfirmed || paymentVerifiedBy==null) throw new IllegalStateException("Payment confirmation required");
        status="approved";visibility="active";reviewedBy=actor;reviewedAt=Instant.now();declineReason=null;
    }
    public void decline(String actor,String reason) {
        requirePending();status="declined";declineReason=reason;reviewedBy=actor;reviewedAt=Instant.now();
    }
    public void suspend(String reason) {
        if(!"approved".equals(status) || !"active".equals(visibility)) throw new IllegalStateException("Only active approved businesses can be suspended");
        visibility="suspended";suspensionReason=reason;
    }
    public void restore() {
        if(!"approved".equals(status) || !"suspended".equals(visibility)) throw new IllegalStateException("Only suspended approved businesses can be restored");
        visibility="active";suspensionReason=null;
    }
    private void requirePending(){if(!"pending".equals(status)) throw new IllegalStateException("Pending request required");}
    public String getId(){return id;} public String getOwnerId(){return ownerId;}
    public String getBusinessName(){return businessName;}public String getCategory(){return category;}
    public String getPhone(){return phone;} public String getWebsite(){return website;}
    public String getDescription(){return description;}public boolean isOnlineOnly(){return onlineOnly;}
    public String getAddress(){return address;}public Double getLatitude(){return latitude;}
    public Double getLongitude(){return longitude;}public String getPlaceId(){return placeId;}
    public String getCity(){return city;} public String getState(){return state;}
    public String getCountryCode(){return countryCode;}
    public String getOpeningHoursJson(){return openingHoursJson;}
    public String getOpeningHoursStructuredJson(){return openingHoursStructuredJson;}
    public String getBannerStorageKey(){return bannerStorageKey;}public String getBannerMime(){return bannerMime;}
    public String getStatus(){return status;}public String getVisibility(){return visibility;}
    public String getDeclineReason(){return declineReason;}public String getSuspensionReason(){return suspensionReason;}
    public Instant getReviewedAt(){return reviewedAt;}public String getReviewedBy(){return reviewedBy;}
    public boolean isPaymentConfirmed(){return paymentConfirmed;}public Instant getPaymentVerifiedAt(){return paymentVerifiedAt;}
    public String getPaymentVerifiedBy(){return paymentVerifiedBy;}
    public String getPaymentReference(){return paymentReference;}public String getPaymentNote(){return paymentNote;}
    public int getRevision(){return revision;}public Instant getSubmittedAt(){return submittedAt;}
    public Instant getLastSubmittedAt(){return lastSubmittedAt;}
}
