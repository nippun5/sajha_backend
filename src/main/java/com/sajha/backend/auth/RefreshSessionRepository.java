package com.sajha.backend.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshSession r where r.tokenHash = :tokenHash")
    Optional<RefreshSession> lockedByTokenHash(@Param("tokenHash") String tokenHash);
    List<RefreshSession> findByFamilyIdAndRevokedAtIsNull(String familyId);
}
