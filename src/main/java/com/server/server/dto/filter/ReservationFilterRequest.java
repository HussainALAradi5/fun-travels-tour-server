package com.server.server.dto.filter;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.server.server.enums.GenericStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReservationFilterRequest extends GenericFilterRequest {
    private GenericStatus status;
    private Long customerId;
    private Long agencyId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
