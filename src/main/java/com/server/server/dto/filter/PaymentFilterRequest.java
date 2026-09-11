package com.server.server.dto.filter;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.server.server.enums.Payment.PaymentMethod;
import com.server.server.enums.Payment.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PaymentFilterRequest extends GenericFilterRequest {
    private Integer userId;
    private PaymentStatus status;
    private PaymentMethod method;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
}
