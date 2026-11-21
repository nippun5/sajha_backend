package com.sajha.backend.marketplace;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/marketplace/listings")
public class ListingController {
    private final ListingService service;
    public ListingController(ListingService service){this.service=service;}

    @GetMapping
    public ListingDtos.PageView browse(@RequestParam(required=false) String kind,
                                       @RequestParam(name="q",required=false) String search,
                                       @RequestParam(defaultValue="0") int page,
                                       @RequestParam(defaultValue="20") int size){
        return service.browse(kind,search,page,size);
    }
    @GetMapping("/{id}")
    public ListingDtos.View details(@PathVariable String id){return service.publicOne(id);}

    @PostMapping
    public ResponseEntity<ListingDtos.View> create(@AuthenticationPrincipal Jwt jwt,
                 @Valid @RequestBody ListingDtos.Create input,
                 @RequestHeader(name="Idempotency-Key",required=false) String idempotencyKey){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(jwt.getSubject(),input,idempotencyKey));
    }
    @PatchMapping("/{id}")
    public ListingDtos.View edit(@AuthenticationPrincipal Jwt jwt,@PathVariable String id,
                                 @Valid @RequestBody ListingDtos.Patch input){
        return service.edit(jwt.getSubject(),id,input);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt,@PathVariable String id){
        service.delete(jwt.getSubject(),id);return ResponseEntity.noContent().build();
    }
}
