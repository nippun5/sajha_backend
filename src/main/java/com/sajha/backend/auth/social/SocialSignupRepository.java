package com.sajha.backend.auth.social;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

public interface SocialSignupRepository extends JpaRepository<SocialSignupChallenge,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SocialSignupChallenge> findByTokenHash(String tokenHash);
}
