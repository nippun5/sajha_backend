package com.sajha.backend.auth.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SocialNonceRepository extends JpaRepository<SocialLoginNonce,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SocialLoginNonce> findByNonceHash(String nonceHash);
}
