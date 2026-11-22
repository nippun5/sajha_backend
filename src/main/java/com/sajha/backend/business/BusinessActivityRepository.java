package com.sajha.backend.business;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BusinessActivityRepository extends JpaRepository<BusinessActivity,String> {
    List<BusinessActivity> findTop200ByOrderByCreatedAtDesc();
}
