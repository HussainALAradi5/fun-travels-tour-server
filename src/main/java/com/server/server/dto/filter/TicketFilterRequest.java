package com.server.server.dto.filter;

import com.server.server.enums.tourmanagement.TicketStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TicketFilterRequest extends GenericFilterRequest {
    private TicketStatus status;
    private Long customerId;
    private Integer tourId;
}
