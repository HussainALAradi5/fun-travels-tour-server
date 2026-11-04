package com.server.server.repositories.payment;

import com.server.server.models.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import com.server.server.enums.payment.PaymentStatus;
public interface PaymentRepository extends JpaRepository<Payment, Integer>, JpaSpecificationExecutor<Payment> {
    

    Optional<Payment> findFirstByReservationIdAndStatus(Integer reservationId, PaymentStatus status);
}
