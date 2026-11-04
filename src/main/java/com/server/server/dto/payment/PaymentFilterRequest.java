package com.server.server.dto.payment;

import com.server.server.dto.common.GenericFilterRequest;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.server.server.enums.payment.PaymentMethod;
import com.server.server.enums.payment.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PaymentFilterRequest extends GenericFilterRequest {
    private Integer userId;
    private PaymentStatus status;
    private PaymentMethod method;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
