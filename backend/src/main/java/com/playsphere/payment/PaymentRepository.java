package com.playsphere.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Payment> findAllByOrderByCreatedAtDesc();
    Optional<Payment> findByProviderOrderId(String providerOrderId);
    boolean existsByPurposeAndReferenceIdAndStatusIn(String purpose, String referenceId, List<String> statuses);
}
