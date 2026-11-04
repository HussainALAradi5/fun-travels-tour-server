package com.server.server.dto.tourmanagement;

import com.server.server.dto.common.GenericFilterRequest;

import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SeatFilterRequest extends GenericFilterRequest {
    private Integer transportId;
    /** When set, availability is reported for this tour (seats held by its tickets are BOOKED). */
    private Integer tourId;
    private SeatStatus status;
    private ChairType chairType;
    private String keyword;
}
