package com.sajha.backend.marketplace;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class ListingDtos {
    private ListingDtos() {}
    public record Create(
        @NotBlank String kind,
        @NotBlank @Size(max=120) String title,
        @NotBlank @Size(max=5000) String description,
        @NotBlank @Pattern(regexp="^\\+[1-9][0-9]{6,14}$") String contactPhone,
        @NotBlank @Size(max=180) String location,
        @NotNull JsonNode details,
        List<String> mediaIds
    ) {}
    public record Patch(
        String title,
        String description,
        String contactPhone,
        String location,
        JsonNode details,
        List<String> mediaIds
    ) {}
    public record View(
        String id, String ownerId, String ownerDisplayName, String kind, String title,
        String description, String contactPhone, String location, List<String> mediaUrls,
        JsonNode details, String status, Instant createdAt, Instant updatedAt, long version
    ) {}
    public record PageView(List<View> items, int page, int size, long totalItems, int totalPages) {}
}
