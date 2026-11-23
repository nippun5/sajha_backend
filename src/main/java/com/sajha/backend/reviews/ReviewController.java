package com.sajha.backend.reviews;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class ReviewController {
    private final ReviewService service;
    public ReviewController(ReviewService service){this.service=service;}

    @GetMapping("/api/v1/businesses/{businessId}/reviews")
    public Map<String,Object> list(@PathVariable String businessId){return service.publicReviews(businessId);}

    @GetMapping("/api/v1/me/businesses/{businessId}/review")
    public Map<String,Object> mine(@AuthenticationPrincipal Jwt jwt,@PathVariable String businessId){
        return service.mine(jwt.getSubject(),businessId);
    }
    @PostMapping("/api/v1/businesses/{businessId}/reviews")
    public Map<String,Object> upsert(@AuthenticationPrincipal Jwt jwt,@PathVariable String businessId,
                                     @RequestBody ReviewDtos.Edit body){
        return service.upsert(jwt.getSubject(),businessId,body);
    }
    @DeleteMapping("/api/v1/me/businesses/{businessId}/review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt,@PathVariable String businessId){
        service.delete(jwt.getSubject(),businessId);
    }
    @PostMapping("/api/v1/businesses/{businessId}/reviews/{reviewId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> report(@AuthenticationPrincipal Jwt jwt,@PathVariable String businessId,
                                     @PathVariable String reviewId,@RequestBody ReviewDtos.Report body){
        return service.report(jwt.getSubject(),businessId,reviewId,body);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/review-reports/{reportId}/moderate")
    public Map<String,Object> moderate(@AuthenticationPrincipal Jwt jwt,@PathVariable String reportId,
                                       @RequestBody ReviewDtos.Moderate body){
        return service.moderate(jwt.getSubject(),reportId,body);
    }
}
