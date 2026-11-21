package com.sajha.backend.marketplace;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class MyListingsController {
    private final ListingService service;
    public MyListingsController(ListingService service){this.service=service;}
    @GetMapping("/listings")
    public ListingDtos.PageView mine(@AuthenticationPrincipal Jwt jwt,
                   @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
        return service.mine(jwt.getSubject(),page,size);
    }
    @GetMapping("/saved-listings")
    public ListingDtos.PageView saved(@AuthenticationPrincipal Jwt jwt,
                   @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
        return service.saved(jwt.getSubject(),page,size);
    }
    @PutMapping("/saved-listings/{listingId}")
    public ResponseEntity<Void> save(@AuthenticationPrincipal Jwt jwt,@PathVariable String listingId){
        service.save(jwt.getSubject(),listingId);return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/saved-listings/{listingId}")
    public ResponseEntity<Void> unsave(@AuthenticationPrincipal Jwt jwt,@PathVariable String listingId){
        service.unsave(jwt.getSubject(),listingId);return ResponseEntity.noContent().build();
    }
}
