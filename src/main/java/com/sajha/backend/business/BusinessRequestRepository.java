package com.sajha.backend.business;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BusinessRequestRepository extends JpaRepository<BusinessRequest,String> {
    List<BusinessRequest> findByOwnerIdOrderByLastSubmittedAtDesc(String ownerId);
    List<BusinessRequest> findAllByOrderByLastSubmittedAtDesc();
    List<BusinessRequest> findByStatusAndVisibilityOrderByLastSubmittedAtDesc(String status,String visibility);
}
