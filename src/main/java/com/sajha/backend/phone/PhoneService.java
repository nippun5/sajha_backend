package com.sajha.backend.phone;

import com.sajha.backend.api.ApiProblem;
import com.sajha.backend.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PhoneService {
    private final PhoneChallengeRepository challenges;
    private final UserRepository users;
    private final VerifiedContactPhoneRepository verified;
    private final PhoneVerifyGateway twilio;
    public PhoneService(PhoneChallengeRepository challenges,UserRepository users,
                        VerifiedContactPhoneRepository verified,PhoneVerifyGateway twilio){
        this.challenges=challenges;this.users=users;this.verified=verified;this.twilio=twilio;
    }
    @Transactional
    public PhoneDtos.Started start(String userId,PhoneDtos.Start input){
        twilio.assertConfigured();
        if(verified.existsByPhone(input.phone()) && !verified.existsByUserIdAndPhone(userId,input.phone()))
            throw new ApiProblem(HttpStatus.CONFLICT,"PHONE_IN_USE","Phone number is already verified by another account");
        Instant now=Instant.now();
        if(challenges.countByUserIdAndCreatedAtAfter(userId,now.minus(1,ChronoUnit.HOURS))>=5 ||
           challenges.countByPhoneAndCreatedAtAfter(input.phone(),now.minus(1,ChronoUnit.HOURS))>=8)
            throw new ApiProblem(HttpStatus.TOO_MANY_REQUESTS,"OTP_RATE_LIMIT","Too many verification requests");
        var recent=challenges.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId,now.minusSeconds(60));
        if(!recent.isEmpty()) throw new ApiProblem(HttpStatus.TOO_MANY_REQUESTS,
            "OTP_COOLDOWN","Wait before requesting another code");
        twilio.send(input.phone());
        PhoneChallenge challenge=challenges.save(new PhoneChallenge(userId,input.phone()));
        return new PhoneDtos.Started(challenge.getId(),challenge.getExpiresAt());
    }
    @Transactional(noRollbackFor=ApiProblem.class)
    public PhoneDtos.Verified verify(String userId,PhoneDtos.Verify input){
        PhoneChallenge challenge=challenges.lockedById(input.challengeId())
            .orElseThrow(()->new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_OTP","Challenge invalid"));
        if(!challenge.getUserId().equals(userId) || challenge.isExpired() || challenge.isCompleted() || challenge.getAttempts()>=5)
            throw new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_OTP","Challenge expired or unavailable");
        twilio.assertConfigured();
        challenge.attempt();
        if(!twilio.check(challenge.getPhone(),input.code()))
            throw new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_OTP","Verification code incorrect");
        AppUser user=users.findById(userId).orElseThrow(()->new ApiProblem(HttpStatus.UNAUTHORIZED,
            "UNAUTHENTICATED","Account not found"));
        if(!verified.existsByUserIdAndPhone(userId,challenge.getPhone())) {
            if(verified.existsByPhone(challenge.getPhone()))
                throw new ApiProblem(HttpStatus.CONFLICT,"PHONE_IN_USE","Phone number already used");
            verified.save(new VerifiedContactPhone(userId,challenge.getPhone()));
        }
        if(user.getPhone().equals(challenge.getPhone())) user.verifyAccountPhone();
        challenge.complete();
        return new PhoneDtos.Verified(user.isPhoneVerified(),challenge.getPhone());
    }
}
