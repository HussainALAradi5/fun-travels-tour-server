package com.server.server.dto.filter;

import com.server.server.enums.GenericStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReservationFilterRequest extends GenericFilterRequest {
    private GenericStatus status;
    private Long customerId;
    private Long agencyId;
}
