package com.sajha.backend.business;

import com.sajha.backend.api.ApiProblem;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;

@RestController
public class BusinessController {
    private final BusinessService service;
    private final JsonMapper mapper;
    public BusinessController(BusinessService service,JsonMapper mapper){this.service=service;this.mapper=mapper;}
    private BusinessDtos.Submission submission(String json){
        try{return mapper.readValue(json,BusinessDtos.Submission.class);}
        catch(JacksonException e){throw new ApiProblem(HttpStatus.BAD_REQUEST,"INVALID_BUSINESS","Invalid business JSON");}
    }
    private ResponseEntity<byte[]> bytes(BusinessImageService.Served image){
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.mime()))
            .header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").body(image.bytes());
    }
    @PostMapping(value="/api/v1/business-requests",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String,Object>> create(@AuthenticationPrincipal Jwt jwt,
        @RequestParam("details") String details,@RequestParam("banner") MultipartFile banner){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(jwt.getSubject(),submission(details),banner));
    }
    @PutMapping(value="/api/v1/me/business-requests/{id}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> resubmit(@AuthenticationPrincipal Jwt jwt,@PathVariable String id,
        @RequestParam("details") String details,@RequestParam("banner") MultipartFile banner){
        return service.resubmit(jwt.getSubject(),id,submission(details),banner);
    }
    @GetMapping("/api/v1/me/business-requests")
    public Map<String,Object> mine(@AuthenticationPrincipal Jwt jwt){
        return Map.of("items",service.mine(jwt.getSubject()));
    }
    @GetMapping("/api/v1/me/business-requests/{id}")
    public Map<String,Object> myDetail(@AuthenticationPrincipal Jwt jwt,@PathVariable String id){
        return service.myDetail(jwt.getSubject(),id);
    }
    @GetMapping("/api/v1/me/business-requests/{id}/banner")
    public ResponseEntity<byte[]> myBanner(@AuthenticationPrincipal Jwt jwt,@PathVariable String id){
        return bytes(service.ownerBanner(jwt.getSubject(),id));
    }
    @GetMapping("/api/v1/businesses")
    public Map<String,Object> publicBusinesses(){return Map.of("items",service.publicList());}
    @GetMapping("/api/v1/businesses/{id}")
    public Map<String,Object> publicDetail(@PathVariable String id){return service.publicDetail(id);}
    @GetMapping("/api/v1/businesses/{id}/banner")
    public ResponseEntity<byte[]> publicBanner(@PathVariable String id){return bytes(service.publicBannerById(id));}
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/business-requests")
    public Map<String,Object> adminSnapshot(){return service.adminSnapshot();}
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/business-requests/{id}/payment")
    public Map<String,Object> payment(@AuthenticationPrincipal Jwt jwt,@PathVariable String id,
        @RequestBody BusinessDtos.Payment body){return service.adminAction(jwt.getSubject(),id,"payment",body,null);}
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/v1/admin/business-requests/{id}/payment")
    public Map<String,Object> revoke(@AuthenticationPrincipal Jwt jwt,@PathVariable String id){
        return service.adminAction(jwt.getSubject(),id,"revoke-payment",null,null);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/business-requests/{id}/approve")
    public Map<String,Object> approve(@AuthenticationPrincipal Jwt jwt,@PathVariable String id){
        return service.adminAction(jwt.getSubject(),id,"approve",null,null);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/business-requests/{id}/decline")
    public Map<String,Object> decline(@AuthenticationPrincipal Jwt jwt,@PathVariable String id,
        @RequestBody BusinessDtos.Reason body){return service.adminAction(jwt.getSubject(),id,"decline",null,body);}
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/business-requests/{id}/suspend")
    public Map<String,Object> suspend(@AuthenticationPrincipal Jwt jwt,@PathVariable String id,
        @RequestBody BusinessDtos.Reason body){return service.adminAction(jwt.getSubject(),id,"suspend",null,body);}
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/business-requests/{id}/restore")
    public Map<String,Object> restore(@AuthenticationPrincipal Jwt jwt,@PathVariable String id){
        return service.adminAction(jwt.getSubject(),id,"restore",null,null);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/business-requests/{id}/banner")
    public ResponseEntity<byte[]> adminBanner(@PathVariable String id){return bytes(service.adminBanner(id));}
}
