package com.sajha.backend.marketplace;

import tools.jackson.databind.JsonNode;
import com.sajha.backend.api.ApiProblem;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Set;

/** Pure validation shared by create and edit; NEVER trust UI-only validation. */
public final class ListingValidation {
    private ListingValidation() {}
    private static final Set<String> KINDS = Set.of("product", "room", "job");
    private static final Set<String> CONDITIONS = Set.of("newItem", "likeNew", "good", "fair", "used");
    private static final Set<String> JOB_TYPES = Set.of("fullTime", "partTime", "casual", "contract", "internship");
    private static final Set<String> CYCLES = Set.of("hourly", "weekly", "monthly", "yearly");
    private static final Set<String> PRODUCT = Set.of("price", "category", "condition", "isNegotiable");
    private static final Set<String> ROOM = Set.of("weeklyRent", "suburb", "state", "postcode", "bedrooms",
        "bathrooms", "furnished", "billsIncluded", "availableFrom", "preferredGender", "amenities");
    private static final Set<String> JOB = Set.of("company", "jobType", "salaryMin", "salaryMax", "salaryCycle",
        "requirements", "closingDate", "isRemote", "employerName");
    private static final String PHONE = "^\\+[1-9][0-9]{6,14}$";

    public static void validateBase(String kind, String title, String description, String contactPhone, String location) {
        if (!KINDS.contains(kind)) fail("kind must be product, room or job");
        required(title, 120, "title");
        required(description, 5000, "description");
        required(location, 180, "location");
        if (contactPhone == null || !contactPhone.matches(PHONE))
            fail("contactPhone must be a valid E.164 phone number");
    }
    private static void required(String value, int limit, String field) {
        if (value == null || value.isBlank() || value.length() > limit) fail(field + " is required (max " + limit + ")");
    }
    private static void text(JsonNode details, String field, int max) {
        var v = details.get(field);
        if (v == null || !v.isTextual() || v.asText().isBlank() || v.asText().length() > max)
            fail(field + " must be nonempty text (max " + max + ")");
    }
    private static void optionalText(JsonNode details, String field, int max) {
        var v=details.get(field);
        if (v!=null && (!v.isTextual() || v.asText().length()>max)) fail(field + " must be text");
    }
    private static void numeric(JsonNode details, String field, boolean required, double minimum) {
        var v=details.get(field);
        if (v==null && !required) return;
        if(v==null || !v.isNumber() || !Double.isFinite(v.asDouble()) || v.asDouble()<minimum || v.asDouble()>1000000000)
            fail(field + " must be a finite number >= " + minimum);
    }
    private static void count(JsonNode details, String field) {
        var v=details.get(field);
        if(v!=null && (!v.isIntegralNumber() || v.asInt()<0 || v.asInt()>100)) fail(field+" must be an integer 0–100");
    }
    private static void flag(JsonNode details, String field) {
        var v=details.get(field);
        if(v!=null && !v.isBoolean()) fail(field+" must be boolean");
    }
    private static void date(JsonNode details, String field) {
        var v=details.get(field);
        if(v==null || v.isNull()) return;
        if(!v.isTextual()) fail(field+" must be a date (YYYY-MM-DD)");
        try { LocalDate.parse(v.asText()); } catch (DateTimeParseException e) { fail(field+" is not a valid date"); }
    }
    private static void option(JsonNode details, String field, Set<String> values, boolean required) {
        var v=details.get(field);
        if(v==null && !required) return;
        if(v==null || !v.isTextual() || !values.contains(v.asText())) fail("Invalid " + field);
    }
    public static void validateDetails(String kind, JsonNode details) {
        if (details==null || !details.isObject() || details.toString().length()>10000)
            fail("details must be a JSON object no larger than 10 KB");
        Set<String> allowed=switch(kind){
            case "product"->PRODUCT;
            case "room"->ROOM;
            case "job"->JOB;
            default->throw new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_KIND","Unknown listing kind");
        };
        Iterator<String> names = details.propertyNames().iterator();
        while(names.hasNext()) {
            String field=names.next();
            if(!allowed.contains(field)) fail("Unexpected details field: " + field);
        }
        switch(kind) {
            case "product" -> {
                numeric(details,"price",true,0.01);
                text(details,"category",60);
                option(details,"condition",CONDITIONS,true);
                flag(details,"isNegotiable");
            }
            case "room" -> {
                numeric(details,"weeklyRent",true,0.01);
                text(details,"suburb",100);
                optionalText(details,"state",80);
                optionalText(details,"postcode",20);
                count(details,"bedrooms"); count(details,"bathrooms");
                flag(details,"furnished"); flag(details,"billsIncluded");
                date(details,"availableFrom");
                optionalText(details,"preferredGender",30);
                var a=details.get("amenities");
                if(a!=null && (!a.isArray() || a.size()>30)) fail("amenities must be an array (max 30)");
                if(a!=null) for(JsonNode item:a) if(!item.isTextual()||item.asText().length()>60) fail("Invalid amenity");
            }
            case "job" -> {
                text(details,"company",100);
                option(details,"jobType",JOB_TYPES,true);
                numeric(details,"salaryMin",false,0);
                numeric(details,"salaryMax",false,0);
                var min=details.get("salaryMin"); var max=details.get("salaryMax");
                if(min!=null && max!=null && max.asDouble()<min.asDouble()) fail("salaryMax must be >= salaryMin");
                option(details,"salaryCycle",CYCLES,false);
                date(details,"closingDate"); flag(details,"isRemote");
                optionalText(details,"employerName",100);
                var reqs=details.get("requirements");
                if(reqs!=null && (!reqs.isArray()||reqs.size()>30)) fail("requirements must be an array (max 30)");
                if(reqs!=null) for(JsonNode item:reqs) if(!item.isTextual()||item.asText().length()>250) fail("Invalid requirement");
            }
            default -> fail("Unsupported kind");
        }
    }
    public static void validateNoMedia(java.util.List<String> mediaIds) {
        if(mediaIds!=null && !mediaIds.isEmpty())
            throw new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"MEDIA_NOT_READY",
                "Listing image uploads are not enabled yet; don't drop selected photos silently");
    }
    private static void fail(String reason) {
        throw new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"VALIDATION_ERROR",reason);
    }
}
