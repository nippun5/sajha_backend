package com.sajha.backend.marketplace;

import com.sajha.backend.api.ApiProblem;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ListingValidationTest {
    private final JsonMapper mapper=JsonMapper.builder().build();

    @Test void productDetailsAcceptedAndPhoneRequired(){
        var details=mapper.readTree("""
            {"price":18,"category":"Home","condition":"good","isNegotiable":true}
            """);
        assertDoesNotThrow(()->ListingValidation.validateDetails("product",details));
        assertDoesNotThrow(()->ListingValidation.validateBase("product","Lamp","Works fine",
            "+13145550123","St Louis, Missouri"));
        assertThrows(ApiProblem.class,()->ListingValidation.validateBase("product","Lamp","Works",
            "","St Louis"));
        assertThrows(ApiProblem.class,()->ListingValidation.validateBase("product","Lamp","Works",
            "123456789","St Louis"));
    }
    @Test void rejectsUnexpectedDetailsAndInvalidPrice(){
        assertThrows(ApiProblem.class,()->ListingValidation.validateDetails("product",
            mapper.readTree("{\"price\":-5,\"category\":\"Home\",\"condition\":\"good\"}")));
        assertThrows(ApiProblem.class,()->ListingValidation.validateDetails("product",
            mapper.readTree("{\"price\":20,\"category\":\"Home\",\"condition\":\"good\",\"ownerId\":\"admin\"}")));
        assertThrows(ApiProblem.class,()->ListingValidation.validateDetails("job",
            mapper.readTree("{\"company\":\"Company\",\"jobType\":\"unknown\"}")));
    }
    @Test void roomAndJobDetailsValidated(){
        assertDoesNotThrow(()->ListingValidation.validateDetails("room",
            mapper.readTree("{\"weeklyRent\":190,\"suburb\":\"Downtown\",\"bedrooms\":1}")));
        assertDoesNotThrow(()->ListingValidation.validateDetails("job",
            mapper.readTree("{\"company\":\"Bakery\",\"jobType\":\"partTime\",\"salaryMin\":20,\"salaryMax\":25}")));
        assertThrows(ApiProblem.class,()->ListingValidation.validateDetails("job",
            mapper.readTree("{\"company\":\"Bakery\",\"jobType\":\"partTime\",\"salaryMin\":30,\"salaryMax\":20}")));
    }
    @Test void mediaCannotBeSilentlyDropped(){
        assertDoesNotThrow(()->ListingValidation.validateNoMedia(List.of()));
        assertThrows(ApiProblem.class,()->ListingValidation.validateNoMedia(List.of("placeholder")));
    }
}
