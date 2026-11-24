package com.sajha.backend.auth.social;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SocialIdentityRepository extends JpaRepository<SocialIdentity,String> {
    Optional<SocialIdentity> findByProviderAndProviderSubject(String provider,String providerSubject);
    boolean existsByUserIdAndProvider(String userId,String provider);
}
