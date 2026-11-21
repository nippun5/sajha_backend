package com.sajha.backend.phone;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PhoneChallengeRepository extends JpaRepository<PhoneChallenge,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PhoneChallenge c where c.id=:id")
    Optional<PhoneChallenge> lockedById(@Param("id") String id);
    List<PhoneChallenge> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(String userId,Instant since);
    long countByUserIdAndCreatedAtAfter(String userId, Instant since);
    long countByPhoneAndCreatedAtAfter(String phone, Instant since);
}
