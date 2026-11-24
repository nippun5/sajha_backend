package com.sajha.backend.recovery;

import com.sajha.backend.api.ApiProblem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Production-only transactional delivery through an optional external email provider.
 * No token is returned to the caller or logged. Unconfigured => fail closed.
 */
@Component
@Profile("!local")
public class ProductionPasswordResetDelivery implements PasswordResetDelivery {
    private final String apiKey,from;
    private final ObjectMapper mapper;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public ProductionPasswordResetDelivery(
        @Value("${SAJHA_RECOVERY_EMAIL_API_KEY:}") String apiKey,
        @Value("${SAJHA_RECOVERY_FROM_EMAIL:}") String from,
        ObjectMapper mapper){this.apiKey=apiKey;this.from=from;this.mapper=mapper;}
    @Override public boolean available(){return !apiKey.isBlank() && !from.isBlank();}
    @Override public void deliver(String email,String token){
        if(!available()) throw new ApiProblem(HttpStatus.SERVICE_UNAVAILABLE,"RECOVERY_UNAVAILABLE","Email recovery not configured");
        try {
            String body=mapper.writeValueAsString(Map.of(
                "from",from,"to",List.of(email),"subject","SaJHa password reset code",
                "text","Your SaJHa password reset code is:\n\n"+token+
                       "\n\nIt expires in 15 minutes. If you did not request this, ignore it."));
            var req=HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .header("Authorization","Bearer "+apiKey)
                .header("Content-Type","application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var result=client.send(req,HttpResponse.BodyHandlers.discarding());
            if(result.statusCode()<200 || result.statusCode()>=300)
                throw new ApiProblem(HttpStatus.BAD_GATEWAY,"EMAIL_DELIVERY_FAILED","Recovery email service unavailable");
        } catch(ApiProblem e){throw e;}
        catch(Exception e){throw new ApiProblem(HttpStatus.BAD_GATEWAY,"EMAIL_DELIVERY_FAILED","Recovery email service unavailable");}
    }
}
