package com.server.server.services;

import com.server.server.enums.Payment.PaymentMethod;
import com.server.server.enums.Payment.PaymentStatus;
import com.server.server.dto.filter.PaymentFilterRequest;
import com.server.server.exceptions.ResourceNotFoundException;
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
import java.util.Set;
import com.server.server.services.filter.GenericFilterService;

@Service
@RequiredArgsConstructor
public class PaymentService extends GenericFilterService<Payment> {
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Payment getById(Integer id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

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
    public List<Payment> filter(PaymentFilterRequest filter) {
        Specification<Payment> spec = Specification.where(null);

        if (filter.getUserId() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("reservation").get("user").get("id"), filter.getUserId()));
        }
        if (filter.getStatus() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.getStatus()));
        }
        if (filter.getMethod() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("method"), filter.getMethod()));
        }
        if (filter.getDate() != null) {
            LocalDateTime start = filter.getDate().atStartOfDay();
            LocalDateTime end = filter.getDate().atTime(LocalTime.MAX);
            spec = spec.and((root, query, cb) -> cb.between(root.get("paymentDate"), start, end));
        }

        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            String term = normalizeSearch(filter.getSearch());
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("transactionId")), term),
                    cb.like(cb.lower(root.get("currency")), term),
                    cb.like(cb.lower(root.get("reservation").get("reservationNumber")), term)));
        }

        return executeFilter(paymentRepository, spec, filter, "paymentDate",
                Set.of("id", "amount", "currency", "method", "status", "transactionId", "paymentDate"));
    }
}
