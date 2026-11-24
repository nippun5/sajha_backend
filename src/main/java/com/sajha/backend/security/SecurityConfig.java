package com.sajha.backend.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import com.sajha.backend.user.UserRepository;
import java.util.Set;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}

    @Bean SecretKey jwtSecret(@Value("${app.auth.jwt-secret-base64:}") String base64){
        if(base64==null||base64.isBlank()) throw new IllegalStateException(
            "JWT_SECRET_BASE64 is required. Generate with: openssl rand -base64 48");
        byte[] secret=Base64.getDecoder().decode(base64.trim());
        if(secret.length<32) throw new IllegalStateException("JWT secret must decode to at least 32 bytes");
        return new SecretKeySpec(secret,"HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey jwtSecret){return NimbusJwtEncoder.withSecretKey(jwtSecret).build();}
    @Bean JwtDecoder jwtDecoder(SecretKey jwtSecret,@Value("${app.auth.issuer}") String issuer,UserRepository users){
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withSecretKey(jwtSecret).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), jwt -> {
                var user=users.findById(jwt.getSubject()).orElse(null);
                Object raw=jwt.getClaim("av");
                var roles=jwt.getClaimAsStringList("roles");
                if(user==null || !(raw instanceof Number) ||
                   ((Number)raw).intValue()!=user.getAuthVersion() ||
                   roles==null || !Set.copyOf(roles).equals(user.getRoles())){
                    return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "access_revoked","Access no longer valid",""));
                }
                return OAuth2TokenValidatorResult.success();
            }));
        return decoder;
    }
    @Bean JwtAuthenticationConverter jwtAuthenticationConverter(){
        var converter=new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt->{
            List<String> roles=jwt.getClaimAsStringList("roles");
            if(roles==null) return List.of();
            return roles.stream().filter(r->r.equals("USER")||r.equals("MODERATOR")||r.equals("ADMIN"))
                .map(r->(GrantedAuthority) new SimpleGrantedAuthority("ROLE_"+r)).toList();
        });
        return converter;
    }
    private void jsonFailure(jakarta.servlet.http.HttpServletResponse res,int status,String code,String message) throws java.io.IOException {
        res.setStatus(status);res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"error\":{\"code\":\""+code+"\",\"message\":\""+message+
            "\",\"details\":[],\"traceId\":\""+UUID.randomUUID()+"\"}}");
    }
    @Bean CorsConfigurationSource adminCorsSource(@Value("${app.admin.allowed-origin:http://127.0.0.1:5173}") String origin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**",config);
        return source;
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,JwtAuthenticationConverter converter,
        CorsConfigurationSource adminCorsSource) throws Exception {
        http.csrf(csrf->csrf.disable()) // Bearer-only mobile API; admin cookie+CSRF will use its own chain in Phase 4C.
            .cors(cors->cors.configurationSource(adminCorsSource)) // No browser app may call these mobile endpoints cross-origin yet.
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a
                .requestMatchers(HttpMethod.GET,"/api/v1/health","/actuator/health").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/auth/register","/api/v1/auth/login","/api/v1/auth/refresh").permitAll()
                // Phase 4C: guests browse; Spring service verifies phone for writes.
                .requestMatchers(HttpMethod.GET,"/api/v1/marketplace/listings",
                    "/api/v1/marketplace/listings/*").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/marketplace/listings").authenticated()
                .requestMatchers(HttpMethod.PATCH,"/api/v1/marketplace/listings/*").authenticated()
                .requestMatchers(HttpMethod.DELETE,"/api/v1/marketplace/listings/*").authenticated()
                // JPEG/PNG files are staged by verified users, served publicly only once attached.
                .requestMatchers(HttpMethod.GET,"/api/v1/media/*").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/media").authenticated()
                .requestMatchers(HttpMethod.GET,"/api/v1/businesses",
                    "/api/v1/businesses/*", "/api/v1/businesses/*/banner").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/business-requests").authenticated()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/me/**","/api/v1/auth/logout").authenticated()
                .requestMatchers(HttpMethod.GET,"/api/v1/businesses/*/reviews").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/businesses/*/reviews",
                    "/api/v1/businesses/*/reviews/*/reports").authenticated()
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/password/forgot", "/api/v1/auth/password/reset").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/auth/social/*", "/api/v1/auth/social/apple/challenge").permitAll()
                .anyRequest().denyAll())
            .oauth2ResourceServer(o->o.jwt(jwt->jwt.jwtAuthenticationConverter(converter)))
            .exceptionHandling(e->e
                .authenticationEntryPoint((req,res,ex)->jsonFailure(res,401,"UNAUTHENTICATED","Authentication required"))
                .accessDeniedHandler((req,res,ex)->jsonFailure(res,403,"FORBIDDEN","Access denied")));
        return http.build();
    }
}
