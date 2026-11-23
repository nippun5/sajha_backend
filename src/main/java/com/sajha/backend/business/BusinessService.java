package com.sajha.backend.business;

import com.sajha.backend.api.ApiProblem;
import com.sajha.backend.reviews.ReviewService;
import com.sajha.backend.user.AppUser;
import com.sajha.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.time.Instant;

@Service
public class BusinessService {
    private final BusinessRequestRepository requests;
    private final BusinessActivityRepository activity;
    private final UserRepository users;
    private final BusinessImageService images;
    private final JsonMapper mapper;
    private final ReviewService reviewService;

    public BusinessService(BusinessRequestRepository requests,BusinessActivityRepository activity,
                           UserRepository users,BusinessImageService images,JsonMapper mapper,ReviewService reviewService){
        this.requests=requests;this.activity=activity;this.users=users;this.images=images;this.mapper=mapper;this.reviewService=reviewService;
    }
    private ApiProblem invalid(String message){return new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_BUSINESS",message);}
    private AppUser user(String id){return users.findById(id).orElseThrow(()->
        new ApiProblem(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Account not found"));}
    private BusinessRequest request(String id){return requests.findById(id).orElseThrow(()->
        new ApiProblem(HttpStatus.NOT_FOUND,"BUSINESS_NOT_FOUND","Business request not found"));}
    private void requireVerified(String id){
        if(!user(id).isPhoneVerified())throw new ApiProblem(HttpStatus.FORBIDDEN,
            "PHONE_NOT_VERIFIED","Verify your phone before submitting a business");
    }
    private static void checkLength(String input,String label,int min,int max){
        if(input==null || input.trim().length()<min || input.trim().length()>max)
            throw new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_BUSINESS",label+" is invalid");
    }
    private void validate(BusinessDtos.Submission f){
        if(f==null)throw invalid("Business details required");
        checkLength(f.name(),"Name",2,100);checkLength(f.category(),"Category",2,60);
        checkLength(f.description(),"Description",10,3000);
        checkLength(f.phone(),"Phone",7,16);
        if(!f.phone().matches("\\+[1-9][0-9]{6,14}"))throw invalid("Use an international business phone number");
        if(f.website()!=null && f.website().length()>300)throw invalid("Website is too long");
        if(!f.onlineOnly()) {
            checkLength(f.address(),"Address",3,250);
            if(f.latitude()==null || f.longitude()==null || !Double.isFinite(f.latitude()) ||
               !Double.isFinite(f.longitude()) || Math.abs(f.latitude())>90 || Math.abs(f.longitude())>180)
                throw invalid("Confirm an address on the map");
        }
        if(f.openingHours()==null || f.openingHours().size()!=7 ||
           f.openingHoursStructured()==null || f.openingHoursStructured().size()!=7)
            throw invalid("Opening hours for all seven days are required");
        Set<String> days=Set.of("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday");
        if(!f.openingHours().keySet().equals(days) || !f.openingHoursStructured().keySet().equals(days))
            throw invalid("Unexpected opening-hours day");
        for(String day:days){
            checkLength(f.openingHours().get(day),day+" hours",1,80);
            var values=f.openingHoursStructured().get(day);
            if(values==null || !List.of("open","closed","allDay").contains(values.get("mode")))
                throw invalid("Invalid opening hours mode");
        }
    }
    private String encode(Object value){
        try {return mapper.writeValueAsString(value);}
        catch(JacksonException ex){throw invalid("Invalid opening hours");}
    }
    @SuppressWarnings("unchecked")
    private Map<String,String> hours(BusinessRequest r){
        try {return (Map<String,String>)mapper.readValue(r.getOpeningHoursJson(),Map.class);}
        catch(JacksonException ex){throw new IllegalStateException("Stored opening hours invalid",ex);}
    }
    @SuppressWarnings("unchecked")
    private Map<String,Map<String,Object>> structured(BusinessRequest r) {
        try {return (Map<String,Map<String,Object>>)mapper.readValue(r.getOpeningHoursStructuredJson(),Map.class);}
        catch(JacksonException ex){throw new IllegalStateException("Stored hours structure invalid",ex);}
    }
    private String publicBanner(String id){return "/api/v1/businesses/"+id+"/banner";}
    @Transactional
    public Map<String,Object> create(String ownerId,BusinessDtos.Submission form,MultipartFile banner){
        requireVerified(ownerId);validate(form);
        BusinessImageService.Stored stored=images.save(banner);
        try {
            BusinessRequest item=requests.saveAndFlush(new BusinessRequest(ownerId,form,
                encode(form.openingHours()),encode(form.openingHoursStructured()),stored.key(),stored.mime()));
            return ownerView(item);
        }catch(RuntimeException e){images.discard(stored);throw e;}
    }
    @Transactional
    public Map<String,Object> resubmit(String ownerId,String requestId,BusinessDtos.Submission form,MultipartFile banner){
        requireVerified(ownerId);validate(form);
        BusinessRequest item=owned(ownerId,requestId);
        if(!"declined".equals(item.getStatus()))throw invalid("Only Declined requests may be corrected");
        BusinessImageService.Stored stored=images.save(banner);
        try {
            item.resubmit(form,encode(form.openingHours()),encode(form.openingHoursStructured()),stored.key(),stored.mime());
            requests.flush();return ownerView(item);
        }catch(RuntimeException e){images.discard(stored);throw e;}
    }
    private BusinessRequest owned(String owner,String id){
        BusinessRequest item=request(id);
        if(!item.getOwnerId().equals(owner))throw new ApiProblem(HttpStatus.NOT_FOUND,
            "BUSINESS_NOT_FOUND","Business request not found");
        return item;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> mine(String owner){
        return requests.findByOwnerIdOrderByLastSubmittedAtDesc(owner).stream().map(this::ownerView).toList();
    }
    @Transactional(readOnly=true)
    public Map<String,Object> myDetail(String owner,String id){return ownerView(owned(owner,id));}
    @Transactional(readOnly=true)
    public Map<String,Object> publicDetail(String id){
        BusinessRequest r=request(id);if(!isPublic(r))throw new ApiProblem(HttpStatus.NOT_FOUND,"BUSINESS_NOT_FOUND","Business not published");
        return publicView(r);
    }
    private boolean isPublic(BusinessRequest r){return "approved".equals(r.getStatus()) && "active".equals(r.getVisibility());}
    @Transactional(readOnly=true)
    public List<Map<String,Object>> publicList(){
        return requests.findByStatusAndVisibilityOrderByLastSubmittedAtDesc("approved","active")
            .stream().map(this::publicView).toList();
    }
    private Map<String,Object> publicView(BusinessRequest r){
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("id",r.getId());m.put("name",r.getBusinessName());m.put("imageUrl",publicBanner(r.getId()));
        m.put("description",r.getDescription());m.put("address",r.getAddress());m.put("phone",r.getPhone());
        m.put("website",r.getWebsite());m.put("category",r.getCategory());
        var rating=reviewService.ratingFor(r.getId());m.put("rating",rating.get("average"));m.put("reviewCount",rating.get("count"));
        m.put("openingHours",hours(r));m.put("latitude",r.getLatitude());m.put("longitude",r.getLongitude());
        m.put("geoapifyPlaceId",r.getPlaceId());m.put("city",r.getCity());m.put("state",r.getState());
        m.put("countryCode",r.getCountryCode());m.put("onlineOnly",r.isOnlineOnly());
        return m;
    }
    private Map<String,Object> ownerView(BusinessRequest r){
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("id",r.getId());m.put("businessName",r.getBusinessName());m.put("category",r.getCategory());
        m.put("phone",r.getPhone());m.put("website",r.getWebsite());m.put("description",r.getDescription());
        m.put("onlineOnly",r.isOnlineOnly());m.put("address",r.getAddress());m.put("latitude",r.getLatitude());
        m.put("longitude",r.getLongitude());m.put("placeId",r.getPlaceId());m.put("city",r.getCity());
        m.put("state",r.getState());m.put("countryCode",r.getCountryCode());m.put("openingHours",hours(r));
        m.put("openingHoursStructured",structured(r));
        m.put("submittedAt",r.getSubmittedAt());m.put("lastSubmittedAt",r.getLastSubmittedAt());
        m.put("revision",r.getRevision());m.put("status",r.getStatus());m.put("visibility",r.getVisibility());
        m.put("declineReason",r.getDeclineReason());m.put("bannerUrl","/api/v1/me/business-requests/"+r.getId()+"/banner");
        return m;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> adminView(BusinessRequest r){
        Map<String,Object> m=new LinkedHashMap<>(ownerView(r));
        AppUser u=user(r.getOwnerId());
        m.put("ownerId",u.getId());m.put("ownerName",u.getName());m.put("ownerEmail",u.getEmail());
        m.put("reviewedAt",r.getReviewedAt());m.put("reviewedBy",r.getReviewedBy());
        m.put("suspensionReason",r.getSuspensionReason());
        Map<String,Object> payment=new LinkedHashMap<>();
        payment.put("confirmed",r.isPaymentConfirmed());payment.put("checkedAt",r.getPaymentVerifiedAt());
        payment.put("verifiedBy",r.getPaymentVerifiedBy());payment.put("reference",r.getPaymentReference());
        payment.put("note",r.getPaymentNote());m.put("payment",payment);
        m.put("bannerUrl","/api/v1/admin/business-requests/"+r.getId()+"/banner");
        return m;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> adminSnapshot(){
        List<Map<String,Object>> rows=requests.findAllByOrderByLastSubmittedAtDesc().stream().map(this::adminView).toList();
        List<Map<String,Object>> history=activity.findTop200ByOrderByCreatedAtDesc().stream().map(e->{
            AppUser actor=user(e.getActorId());
            return Map.<String,Object>of("id",e.getId(),"requestId",e.getRequestId(),"at",e.getCreatedAt(),
                "actor",actor.getName(),"type",e.getActionType(),"detail",e.getDetail());
        }).toList();
        return Map.of("requests",rows,"reports",reviewService.adminReports(),"activity",history);
    }
    private void event(BusinessRequest r,String actor,String type,String detail){
        activity.save(new BusinessActivity(r.getId(),actor,type,detail));
    }
    @Transactional
    public Map<String,Object> adminAction(String actorId,String id,String action,BusinessDtos.Payment payment,BusinessDtos.Reason reason){
        user(actorId);BusinessRequest r=request(id);
        try {
            switch(action){
                case "payment" -> {
                    if(payment==null || !payment.confirmed())throw invalid("Explicit payment confirmation is required");
                    String reference=payment.reference()==null?"":payment.reference().trim();
                    String note=payment.note()==null?"":payment.note().trim();
                    if(reference.length()>100 || note.length()>400)throw invalid("Payment note/reference too long");
                    r.confirmPayment(actorId,reference,note);event(r,actorId,"payment","Payment manually confirmed");
                }
                case "revoke-payment" -> {r.clearPayment();event(r,actorId,"payment","Payment confirmation revoked");}
                case "approve" -> {r.approve(actorId);event(r,actorId,"approve","Business approved for publication");}
                case "decline" -> {
                    String why=reason==null?"":reason.reason();checkLength(why,"Decline reason",10,800);
                    r.decline(actorId,why.trim());event(r,actorId,"decline","Business request declined");
                }
                case "suspend" -> {
                    String why=reason==null?"":reason.reason();checkLength(why,"Suspension reason",8,800);
                    r.suspend(why.trim());event(r,actorId,"suspend","Business publication suspended");
                }
                case "restore" -> {r.restore();event(r,actorId,"restore","Business publication restored");}
                default -> throw new ApiProblem(HttpStatus.NOT_FOUND,"ACTION_NOT_FOUND","Unknown admin action");
            }
        }catch(IllegalStateException ex){throw new ApiProblem(HttpStatus.CONFLICT,"INVALID_STATE",ex.getMessage());}
        requests.flush();return adminView(r);
    }
    @Transactional(readOnly=true)
    public BusinessImageService.Served ownerBanner(String ownerId,String id){return images.load(owned(ownerId,id));}
    @Transactional(readOnly=true)
    public BusinessImageService.Served adminBanner(String id){return images.load(request(id));}
    @Transactional(readOnly=true)
    public BusinessImageService.Served publicBannerById(String id){
        BusinessRequest r=request(id);
        if(!isPublic(r))throw new ApiProblem(HttpStatus.NOT_FOUND,"IMAGE_NOT_FOUND","Business banner unavailable");
        return images.load(r);
    }
}
