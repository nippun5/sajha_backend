package com.sajha.backend.reviews;

import com.sajha.backend.api.ApiProblem;
import com.sajha.backend.business.BusinessActivity;
import com.sajha.backend.business.BusinessActivityRepository;
import com.sajha.backend.business.BusinessRequest;
import com.sajha.backend.business.BusinessRequestRepository;
import com.sajha.backend.user.AppUser;
import com.sajha.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ReviewService {
    private static final Set<String> REASONS = Set.of(
        "Spam or advertising", "Harassment or abusive language", "Fake or misleading review",
        "Conflict of interest", "Other");
    private final BusinessReviewRepository reviews;
    private final BusinessReviewReportRepository reports;
    private final BusinessRequestRepository businesses;
    private final UserRepository users;
    private final BusinessActivityRepository activity;

    public ReviewService(BusinessReviewRepository reviews,BusinessReviewReportRepository reports,
                         BusinessRequestRepository businesses,UserRepository users,BusinessActivityRepository activity){
        this.reviews=reviews;this.reports=reports;this.businesses=businesses;this.users=users;this.activity=activity;
    }
    private ApiProblem error(HttpStatus status,String code,String message){return new ApiProblem(status,code,message);}
    private BusinessRequest business(String id){return businesses.findById(id).orElseThrow(()->
        error(HttpStatus.NOT_FOUND,"BUSINESS_NOT_FOUND","Business does not exist"));}
    private BusinessRequest publicBusiness(String id){
        BusinessRequest b=business(id);
        if(!"approved".equals(b.getStatus()) || !"active".equals(b.getVisibility()))
            throw error(HttpStatus.NOT_FOUND,"BUSINESS_NOT_FOUND","Business is not published");
        return b;
    }
    private AppUser verified(String id){
        AppUser user=users.findById(id).orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Account not found"));
        if(!user.isPhoneVerified())throw error(HttpStatus.FORBIDDEN,"PHONE_NOT_VERIFIED","Verify phone to review or report");
        return user;
    }
    private static String validComment(String comment){
        String clean=comment==null?"":comment.trim();
        if(clean.length()<10 || clean.length()>600)
            throw new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_REVIEW","Review must be 10–600 characters");
        return clean;
    }
    private static void validStars(int stars){
        if(stars<1 || stars>5)throw new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_RATING","Choose 1–5 stars");
    }
    @Transactional(readOnly=true)
    public Map<String,Object> ratingFor(String businessId){
        List<BusinessReview> active=reviews.findByBusinessIdAndStatusOrderByUpdatedAtDesc(businessId,"active");
        double avg=active.stream().mapToInt(BusinessReview::getStars).average().orElse(0);
        return Map.of("average",avg,"count",active.size());
    }
    private Map<String,Object> view(BusinessReview item){
        AppUser author=users.findById(item.getAuthorId()).orElseThrow(()->
            error(HttpStatus.INTERNAL_SERVER_ERROR,"AUTHOR_MISSING","Review author missing"));
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("id",item.getId());result.put("businessId",item.getBusinessId());
        result.put("authorId",item.getAuthorId());result.put("authorName",author.getName());
        result.put("stars",item.getStars());result.put("comment",item.getComment());
        result.put("createdAt",item.getCreatedAt());result.put("updatedAt",item.getUpdatedAt());
        return result;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> publicReviews(String businessId){
        publicBusiness(businessId);
        var rows=reviews.findByBusinessIdAndStatusOrderByUpdatedAtDesc(businessId,"active");
        double avg=rows.stream().mapToInt(BusinessReview::getStars).average().orElse(0);
        return Map.of("items",rows.stream().map(this::view).toList(),
                      "summary",Map.of("average",avg,"count",rows.size()));
    }
    @Transactional(readOnly=true)
    public Map<String,Object> mine(String actorId,String businessId){
        publicBusiness(businessId);
        BusinessReview r=reviews.findByBusinessIdAndAuthorId(businessId,actorId).orElse(null);
        return Collections.singletonMap("review",r!=null && "active".equals(r.getStatus())?view(r):null);
    }
    @Transactional
    public Map<String,Object> upsert(String actorId,String businessId,ReviewDtos.Edit body){
        verified(actorId);BusinessRequest b=publicBusiness(businessId);
        if(b.getOwnerId().equals(actorId))
            throw error(HttpStatus.FORBIDDEN,"OWNER_SELF_REVIEW","Business owners cannot review their own business");
        if(body==null)throw error(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_REVIEW","Review details required");
        validStars(body.stars());String clean=validComment(body.comment());
        BusinessReview current=reviews.findByBusinessIdAndAuthorId(businessId,actorId).orElse(null);
        if(current!=null && "removed".equals(current.getStatus()))
            throw error(HttpStatus.FORBIDDEN,"REVIEW_REMOVED","A moderated review cannot be republished by this account");
        if(current==null)current=new BusinessReview(businessId,actorId,body.stars(),clean);
        else current.setReview(body.stars(),clean);
        return view(reviews.saveAndFlush(current));
    }
    @Transactional
    public void delete(String actorId,String businessId){
        verified(actorId);
        BusinessReview review=reviews.findByBusinessIdAndAuthorId(businessId,actorId).orElseThrow(()->
            error(HttpStatus.NOT_FOUND,"REVIEW_NOT_FOUND","Your review does not exist"));
        if(!"active".equals(review.getStatus()))
            throw error(HttpStatus.CONFLICT,"REVIEW_NOT_ACTIVE","Review is not active");
        review.delete();reviews.flush();
    }
    @Transactional
    public Map<String,Object> report(String actorId,String businessId,String reviewId,ReviewDtos.Report body){
        verified(actorId);publicBusiness(businessId);
        String reason=body==null?null:body.reason();
        if(reason==null || !REASONS.contains(reason))throw error(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_REASON","Select a report reason");
        BusinessReview review=reviews.findById(reviewId).orElseThrow(()->
            error(HttpStatus.NOT_FOUND,"REVIEW_NOT_FOUND","Review not found"));
        if(!review.getBusinessId().equals(businessId) || !"active".equals(review.getStatus()))
            throw error(HttpStatus.NOT_FOUND,"REVIEW_NOT_FOUND","Review not found");
        if(review.getAuthorId().equals(actorId))
            throw error(HttpStatus.FORBIDDEN,"SELF_REPORT","You cannot report your own review");
        if(reports.findByReviewIdAndReporterId(reviewId,actorId).isPresent())
            throw error(HttpStatus.CONFLICT,"REPORT_EXISTS","Review already reported by this account");
        BusinessReviewReport saved=reports.saveAndFlush(new BusinessReviewReport(reviewId,actorId,reason));
        return Map.of("id",saved.getId(),"status",saved.getStatus());
    }
    private Map<String,Object> adminView(BusinessReviewReport r){
        BusinessReview review=reviews.findById(r.getReviewId()).orElseThrow(()->
            error(HttpStatus.INTERNAL_SERVER_ERROR,"REVIEW_MISSING","Review missing"));
        BusinessRequest b=business(review.getBusinessId());
        AppUser author=users.findById(review.getAuthorId()).orElseThrow(()->
            error(HttpStatus.INTERNAL_SERVER_ERROR,"AUTHOR_MISSING","Review author missing"));
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("id",r.getId());m.put("businessId",b.getId());m.put("businessName",b.getBusinessName());
        m.put("reviewId",review.getId());m.put("reviewer",author.getName());
        m.put("stars",review.getStars());m.put("reviewText",review.getComment());
        m.put("reporterId",r.getReporterId());m.put("reason",r.getReason());
        m.put("createdAt",r.getCreatedAt());m.put("status",r.getStatus());
        m.put("moderatedAt",r.getModeratedAt());m.put("moderatorNote",r.getModeratorNote());
        return m;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> adminReports(){
        return reports.findAllByOrderByCreatedAtDesc().stream().map(this::adminView).toList();
    }
    @Transactional
    public Map<String,Object> moderate(String actorId,String reportId,ReviewDtos.Moderate body){
        AppUser actor=users.findById(actorId).orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Account not found"));
        if(!actor.getRoles().contains("ADMIN"))throw error(HttpStatus.FORBIDDEN,"FORBIDDEN","Admin required");
        if(body==null || !Set.of("dismissed","removed").contains(body.decision()))
            throw error(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_DECISION","Choose dismissed or removed");
        String note=body.note()==null?"":body.note().trim();
        if(note.length()<10 || note.length()>800)
            throw error(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_NOTE","Moderation note must be 10–800 characters");
        BusinessReviewReport report=reports.findById(reportId).orElseThrow(()->
            error(HttpStatus.NOT_FOUND,"REPORT_NOT_FOUND","Report does not exist"));
        if(!"open".equals(report.getStatus()))
            throw error(HttpStatus.CONFLICT,"REPORT_CLOSED","Report is already resolved");
        BusinessReview review=reviews.findById(report.getReviewId()).orElseThrow(()->
            error(HttpStatus.NOT_FOUND,"REVIEW_NOT_FOUND","Review not found"));
        if("removed".equals(body.decision())){
            review.moderateRemove();
            // Resolve all open reports concerning the same review so none remain actionable.
            for(var pending:reports.findByReviewIdAndStatus(review.getId(),"open")){
                pending.resolve(actorId,"removed",note);
            }
        } else report.resolve(actorId,"dismissed",note);
        activity.save(new BusinessActivity(review.getBusinessId(),actorId,"review_report",
            "Review report "+reportId+" marked "+body.decision()));
        reports.flush();reviews.flush();
        return adminView(report);
    }
}
