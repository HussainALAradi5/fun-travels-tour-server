package com.server.server.services.payment;

import com.server.server.constants.BookingRules;

import com.server.server.constants.CodePrefixes;

import com.server.server.utilities.CodeGenerator;

import com.server.server.models.user.User;

import com.server.server.exceptions.AccessDeniedException;

import com.server.server.services.auth.AccessPolicyService;

import com.server.server.enums.payment.PaymentMethod;
import com.server.server.enums.payment.PaymentStatus;
import com.server.server.dto.payment.PaymentFilterRequest;
import com.server.server.dto.common.PageResponse;
import com.server.server.exceptions.ResourceNotFoundException;
import com.server.server.models.payment.Payment;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.repositories.payment.PaymentRepository;

import com.server.server.enums.payment.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import com.server.server.services.common.GenericFilterService;
import com.server.server.utilities.FilterUtils;
import com.server.server.exceptions.WorkflowException;
@Service
@RequiredArgsConstructor
public class PaymentService extends GenericFilterService<Payment> {
    private final PaymentRepository paymentRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AccessPolicyService accessPolicy;

    @Transactional(readOnly = true)
    public Payment getById(Integer id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
        TourReservation reservation = payment.getReservation();
        User current = accessPolicy.requireCurrentUser();
        if (reservation == null) {
            if (!AccessPolicyService.isAdmin(current)) {
                throw new AccessDeniedException("Only administrators can view wallet top-up payments.");
            }
        } else {
            accessPolicy.requireRecordAccess(current, reservation.getUser().getId(), reservation.getTour().getAgency());
        }
        return payment;
    }

    /**
     * Creates and executes a transaction.
     */
    @Transactional
    public Payment executeTransaction(TourReservation res, PaymentMethod method) {
        return paymentRepository.findFirstByReservationIdAndStatus(res.getId(), PaymentStatus.COMPLETED)
                .orElseGet(() -> createPayment(res, method));
    }

    private Payment createPayment(TourReservation res, PaymentMethod method) {
        Payment payment = Payment.builder()
                .reservation(res)
                .amount(res.getTotalPrice())
                .currency(BookingRules.CURRENCY)
                .method(method)
                .status(PaymentStatus.PENDING)
                .transactionId(CodeGenerator.generate(CodePrefixes.PAYMENT))
                .build();

        // Bookings are paid from the wallet; cards top the wallet up first. There is no provider
        // webhook to complete an external booking payment, so other methods would stay pending forever.
        if (method != PaymentMethod.WALLET) {
            throw new WorkflowException("PAYMENT_METHOD_UNSUPPORTED",
                    "Bookings are paid from your wallet. Top up your wallet by card, then pay.");
        }
        var account = accountService.getAccountByUserId(res.getUser().getId());
        transactionService.debitAccount(account, res.getTotalPrice(), TransactionType.PAYMENT,
                "Payment for reservation " + res.getReservationNumber(), res);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    /**
     * Dynamic filtering with Date support and DESC sorting.
     */
    @Transactional(readOnly = true)
    public PageResponse<Payment> filter(PaymentFilterRequest filter) {
        FilterUtils.validateRange(filter.getStartDate(), filter.getEndDate(), "startDate", "endDate");
        if (filter.getDate() != null && (filter.getStartDate() != null || filter.getEndDate() != null)) {
            throw new IllegalArgumentException("Use either date or startDate/endDate, not both.");
        }
        User current = accessPolicy.requireCurrentUser();
        Specification<Payment> spec = Specification.unrestricted();
        if (AccessPolicyService.isCustomer(current)) {
            filter.setUserId(current.getId());
        }
        Integer agencyScope = AccessPolicyService.agencyScope(current);
        if (agencyScope != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reservation").get("tour").get("agency").get("id"), agencyScope));
        }

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
        } else {
            if (filter.getStartDate() != null) {
                LocalDateTime start = filter.getStartDate().atStartOfDay();
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("paymentDate"), start));
            }
            if (filter.getEndDate() != null) {
                LocalDateTime endExclusive = filter.getEndDate().plusDays(1).atStartOfDay();
                spec = spec.and((root, query, cb) -> cb.lessThan(root.get("paymentDate"), endExclusive));
            }
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
