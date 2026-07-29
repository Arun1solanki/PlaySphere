package com.playsphere.payment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, String> {
    List<RefundRequest> findByStatusOrderByCreatedAtAsc(String status);
    List<RefundRequest> findByRequestedByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByPaymentIdAndStatus(String paymentId, String status);
}
