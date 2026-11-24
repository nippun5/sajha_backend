package com.sajha.backend.auth.social;

import com.sajha.backend.api.ApiProblem;
import com.sajha.backend.util.SecurityStrings;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialNonceService {
    public record Challenge(String nonce,int expiresIn) {}
    private final SocialNonceRepository repository;
    public SocialNonceService(SocialNonceRepository repository){this.repository=repository;}
    @Transactional public Challenge issue(){
        String nonce=SecurityStrings.randomToken();
        repository.save(new SocialLoginNonce(SecurityStrings.sha256(nonce)));
        return new Challenge(nonce,300);
    }
    @Transactional public void consume(String nonce){
        if(nonce==null || nonce.isBlank()) throw invalid();
        var entry=repository.findByNonceHash(SecurityStrings.sha256(nonce)).orElseThrow(SocialNonceService::invalid);
        if(!entry.valid()) throw invalid();
        entry.consume();
    }
    private static ApiProblem invalid(){return new ApiProblem(HttpStatus.UNAUTHORIZED,"INVALID_OAUTH_NONCE","Sign-in challenge expired. Retry.");}
}
