package com.server.server.dto.tourmanagement;

import com.server.server.dto.common.GenericFilterRequest;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.server.server.enums.common.GenericStatus;
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
