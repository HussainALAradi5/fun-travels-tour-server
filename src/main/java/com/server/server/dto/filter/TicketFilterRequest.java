package com.server.server.dto.filter;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.server.server.enums.tourmanagement.TicketStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TicketFilterRequest extends GenericFilterRequest {
    private TicketStatus status;
    private Long customerId;
    private Integer tourId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
