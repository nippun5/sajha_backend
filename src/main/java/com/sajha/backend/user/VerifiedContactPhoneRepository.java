package com.sajha.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VerifiedContactPhoneRepository extends JpaRepository<VerifiedContactPhone,String> {
    List<VerifiedContactPhone> findByUserId(String userId);
    boolean existsByPhone(String phone);
    boolean existsByUserIdAndPhone(String userId,String phone);
}
