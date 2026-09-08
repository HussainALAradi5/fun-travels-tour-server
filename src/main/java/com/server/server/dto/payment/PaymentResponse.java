package com.server.server.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.server.server.enums.Payment.PaymentMethod;
import com.server.server.enums.Payment.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private Integer id;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private PaymentStatus status;
    private String transactionId;
    private ReservationSummary reservation;
    private LocalDateTime paymentDate;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ReservationSummary {
        private Integer id;
        private String reservationNumber;
    }
}
