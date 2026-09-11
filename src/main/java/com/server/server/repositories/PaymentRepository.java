package com.server.server.repositories;

import com.server.server.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer>, JpaSpecificationExecutor<Payment> {
    
    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByReservationId(Integer reservationId);
}
