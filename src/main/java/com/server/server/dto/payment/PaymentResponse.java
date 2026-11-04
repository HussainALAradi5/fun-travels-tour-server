package com.server.server.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.server.server.dto.tourmanagement.ReservationSummary;
import com.server.server.enums.payment.PaymentMethod;
import com.server.server.enums.payment.PaymentStatus;
import com.server.server.models.payment.Payment;
public record PaymentResponse(
        Integer id,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        PaymentStatus status,
        String transactionId,
        ReservationSummary reservation,
        LocalDateTime paymentDate) {

    public static PaymentResponse from(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(payment.getId(), payment.getAmount(), payment.getCurrency(), payment.getMethod(),
                payment.getStatus(), payment.getTransactionId(), ReservationSummary.from(payment.getReservation()),
                payment.getPaymentDate());
    }
}
