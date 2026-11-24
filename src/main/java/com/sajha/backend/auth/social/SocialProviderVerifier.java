package com.sajha.backend.auth.social;

import com.sajha.backend.api.ApiProblem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Verification happens ONLY on the server; neither client email nor client role is trusted.
 * Google/Apple: JWKS signature + expiry + issuer + audience + verified email.
 * Meta: app-bound debug_token + /me subject match; Facebook email is not treated as verified.
 */
@Component
public class SocialProviderVerifier {
    public record Profile(String provider,String subject,String email,String name,boolean emailVerified) {}
    private final Set<String> googleClientIds, appleClientIds;
    private final String facebookAppId, facebookAppSecret;
    private final JwtDecoder googleJwt, appleJwt;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ObjectMapper mapper;

    public SocialProviderVerifier(
        @Value("${SAJHA_GOOGLE_CLIENT_IDS:}") String googleIds,
        @Value("${SAJHA_APPLE_CLIENT_IDS:}") String appleIds,
        @Value("${SAJHA_FACEBOOK_APP_ID:}") String facebookId,
        @Value("${SAJHA_FACEBOOK_APP_SECRET:}") String facebookSecret,
        ObjectMapper mapper){
        this.googleClientIds=parse(googleIds);this.appleClientIds=parse(appleIds);
        this.facebookAppId=facebookId;this.facebookAppSecret=facebookSecret;
        this.mapper=mapper;
        NimbusJwtDecoder gd=NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
        gd.setJwtValidator(JwtValidators.createDefault());googleJwt=gd;
        NimbusJwtDecoder ad=NimbusJwtDecoder.withJwkSetUri("https://appleid.apple.com/auth/keys").build();
        ad.setJwtValidator(JwtValidators.createDefault());appleJwt=ad;
    }
    private static Set<String> parse(String text){return Arrays.stream(text.split(","))
        .map(String::trim).filter(s->!s.isEmpty()).collect(Collectors.toUnmodifiableSet());}
    public Profile verify(String provider,String credential,String nonce){
        if(credential==null || credential.isBlank() || credential.length()>12000) throw badToken();
        return switch(provider.toLowerCase(Locale.ROOT)){
            case "google" -> verifyJwt("google", credential,googleClientIds,googleJwt,Set.of("accounts.google.com","https://accounts.google.com"),null);
            case "apple" -> verifyJwt("apple", credential,appleClientIds,appleJwt,Set.of("https://appleid.apple.com"),nonce);
            case "facebook" -> verifyFacebook(credential);
            default -> throw new ApiProblem(HttpStatus.BAD_REQUEST,"UNKNOWN_PROVIDER","Unsupported sign-in provider");
        };
    }
    private static ApiProblem badToken(){return new ApiProblem(HttpStatus.UNAUTHORIZED,"SOCIAL_TOKEN_INVALID","Social sign-in failed");}
    private static void requireConfigured(boolean ok){if(!ok)throw new ApiProblem(HttpStatus.SERVICE_UNAVAILABLE,"PROVIDER_UNAVAILABLE","Sign-in provider is not configured");}
    private Profile verifyJwt(String provider,String token,Set<String> clients,JwtDecoder decoder,Set<String> issuers,String nonce){
        requireConfigured(!clients.isEmpty());
        try {
            Jwt jwt=decoder.decode(token);
            if(!issuers.contains(jwt.getIssuer().toString()) || jwt.getAudience().stream().noneMatch(clients::contains)) throw badToken();
            if(provider.equals("apple") && (nonce==null || nonce.isBlank() ||
                !nonce.equals(jwt.getClaimAsString("nonce")))) throw badToken();
            String subject=jwt.getSubject();
            String email=jwt.getClaimAsString("email");
            Object ev=jwt.getClaim("email_verified");
            boolean verified=Boolean.TRUE.equals(ev) || "true".equalsIgnoreCase(String.valueOf(ev));
            if(subject==null || subject.isBlank() || subject.length()>255 || email==null || !verified) throw badToken();
            if(provider.equals("google")) {
                var aud=jwt.getAudience();
                String azp=jwt.getClaimAsString("azp");
                if(aud.size()>1 && (azp==null || !clients.contains(azp))) throw badToken();
            }
            return new Profile(provider,subject,email.toLowerCase(Locale.ROOT),jwt.getClaimAsString("name"),true);
        } catch(JwtException | NullPointerException e){throw badToken();}
    }
    private Profile verifyFacebook(String token){
        requireConfigured(!facebookAppId.isBlank() && !facebookAppSecret.isBlank());
        try {
            String debug=UriComponentsBuilder.fromUriString("https://graph.facebook.com/debug_token")
                .queryParam("input_token",token).build().encode().toUriString();
            var req=HttpRequest.newBuilder(URI.create(debug))
                .header("Authorization","Bearer "+facebookAppId+"|"+facebookAppSecret)
                .timeout(Duration.ofSeconds(8)).GET().build();
            var response=http.send(req,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200) throw badToken();
            var data=mapper.readTree(response.body()).path("data");
            boolean valid=data.path("is_valid").asBoolean(false);
            String appId=data.path("app_id").asText("");
            String userId=data.path("user_id").asText("");
            long exp=data.path("expires_at").asLong(0);
            if(!valid || !appId.equals(facebookAppId) || userId.isBlank() || exp<=Instant.now().getEpochSecond()) throw badToken();
            var me=HttpRequest.newBuilder(URI.create("https://graph.facebook.com/me?fields=id,name,email"))
                .header("Authorization","Bearer "+token)
                .timeout(Duration.ofSeconds(8)).GET().build();
            var profileResponse=http.send(me,HttpResponse.BodyHandlers.ofString());
            if(profileResponse.statusCode()!=200) throw badToken();
            var body=mapper.readTree(profileResponse.body());
            if(!userId.equals(body.path("id").asText(""))) throw badToken();
            // Meta /me email is NOT a verified-email assertion. Linking is allowed, auto-signup is not.
            return new Profile("facebook",userId,body.path("email").asText(""),body.path("name").asText(""),false);
        } catch (ApiProblem e){throw e;} catch(Exception e){throw badToken();}
    }
}
