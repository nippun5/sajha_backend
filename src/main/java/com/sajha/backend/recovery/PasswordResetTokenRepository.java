package com.sajha.backend.recovery;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(String userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.tokenHash = :tokenHash")
    Optional<PasswordResetToken> lockByTokenHash(@Param("tokenHash") String tokenHash);
}
