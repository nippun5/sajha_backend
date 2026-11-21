package com.sajha.backend.phone;

import com.sajha.backend.api.ApiProblem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/** Twilio Verify v2. Disabled without real provider credentials; never generates demo OTPs. */
@Component
@Profile("!local")
public class TwilioVerifyClient implements PhoneVerifyGateway {
    private final String accountSid,authToken,serviceSid;
    private final JsonMapper json;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public TwilioVerifyClient(@Value("${app.sms.twilio-account-sid:}") String accountSid,
                              @Value("${app.sms.twilio-auth-token:}") String authToken,
                              @Value("${app.sms.twilio-verify-service-sid:}") String serviceSid,
                              JsonMapper json){
        this.accountSid=accountSid;this.authToken=authToken;this.serviceSid=serviceSid;this.json=json;
    }
    public void assertConfigured(){
        if(accountSid.isBlank()||authToken.isBlank()||serviceSid.isBlank())
            throw new ApiProblem(HttpStatus.SERVICE_UNAVAILABLE,"SMS_NOT_CONFIGURED",
                "Phone verification provider is not configured");
    }
    private static String formValue(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private String post(String resource,String body){
        assertConfigured();
        String auth=Base64.getEncoder().encodeToString((accountSid+":"+authToken)
            .getBytes(StandardCharsets.UTF_8));
        var req=HttpRequest.newBuilder(URI.create("https://verify.twilio.com/v2/Services/"+serviceSid+"/"+resource))
            .timeout(Duration.ofSeconds(12))
            .header("Authorization","Basic "+auth)
            .header("Content-Type","application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try {
            var response=http.send(req,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()==429) throw new ApiProblem(HttpStatus.TOO_MANY_REQUESTS,
                "SMS_LIMIT","Phone verification temporarily limited");
            if(response.statusCode()>=400) {
                if(resource.equals("VerificationCheck") && response.statusCode()==404)
                    return ""; // Twilio returns not found for expired/invalid checks.
                throw new ApiProblem(HttpStatus.BAD_GATEWAY,"SMS_PROVIDER_ERROR",
                    "Phone verification provider rejected the request");
            }
            return json.readTree(response.body()).path("status").asText();
        } catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new ApiProblem(HttpStatus.BAD_GATEWAY,"SMS_PROVIDER_ERROR","Phone verification unavailable");
        } catch(java.io.IOException e){
            throw new ApiProblem(HttpStatus.BAD_GATEWAY,"SMS_PROVIDER_ERROR","Phone verification unavailable");
        }
    }
    public void send(String phone){
        String status=post("Verifications","To="+formValue(phone)+"&Channel=sms");
        if(!status.equals("pending")&&!status.equals("approved"))
            throw new ApiProblem(HttpStatus.BAD_GATEWAY,"SMS_PROVIDER_ERROR","Phone verification could not start");
    }
    public boolean check(String phone,String code){
        String status=post("VerificationCheck","To="+formValue(phone)+"&Code="+formValue(code));
        return status.equals("approved");
    }
}
