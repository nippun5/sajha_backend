package com.sajha.backend.business;
import java.util.Map;

public final class BusinessDtos {
    private BusinessDtos(){}
    public record Submission(String name,String category,String phone,String website,String description,
                             boolean onlineOnly,String address,Double latitude,Double longitude,
                             String placeId,String city,String state,String countryCode,
                             Map<String,String> openingHours,
                             Map<String,Map<String,Object>> openingHoursStructured) {}
    public record Payment(boolean confirmed,String reference,String note) {}
    public record Reason(String reason) {}
}
