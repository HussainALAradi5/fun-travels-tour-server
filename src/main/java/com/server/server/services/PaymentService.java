package com.server.server.services;

import com.server.server.enums.Payment.PaymentMethod;
import com.server.server.enums.Payment.PaymentStatus;
import com.server.server.models.Payment;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    /**
     * Creates and executes a transaction.
     */
    @Transactional
    public Payment executeTransaction(TourReservation res, PaymentMethod method) {
        Payment payment = Payment.builder()
                .reservation(res)
                .amount(res.getTotalPrice())
                .currency("USD")
                .method(method)
                .status(PaymentStatus.PENDING)
                .transactionId("TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();

        // 90% Success simulation
        boolean isSuccessful = Math.random() > 0.1;

        if (isSuccessful) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaymentDate(LocalDateTime.now());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        return paymentRepository.save(payment);
    }

    /**
     * Dynamic filtering with Date support and DESC sorting.
     */
    @Transactional(readOnly = true)
    public List<Payment> filter(Integer userId, PaymentStatus status, PaymentMethod method, LocalDate date) {
        Specification<Payment> spec = Specification.where(null);

        if (userId != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("reservation").get("user").get("id"), userId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (method != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("method"), method));
        }
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            spec = spec.and((root, query, cb) -> cb.between(root.get("paymentDate"), start, end));
        }

        return paymentRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "paymentDate"));
    }
}