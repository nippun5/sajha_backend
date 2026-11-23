package com.sajha.backend.reviews;

public final class ReviewDtos {
    private ReviewDtos() {}
    public record Edit(int stars, String comment) {}
    public record Report(String reason) {}
    public record Moderate(String decision,String note) {}
}
