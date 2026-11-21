package com.sajha.backend.marketplace.media;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/media")
public class MarketplaceMediaController {
    private final MarketplaceMediaService service;
    public MarketplaceMediaController(MarketplaceMediaService service){this.service=service;}

    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String,Object>> upload(@AuthenticationPrincipal Jwt jwt,
                                        @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.stage(jwt.getSubject(),file));
    }
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> image(@PathVariable String id) {
        var image=service.publicImage(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType()))
            .cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options","nosniff")
            .body(image.body());
    }
}
