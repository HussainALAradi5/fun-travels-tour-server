package com.server.server.dto.tourmanagement;

import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;

import lombok.Data;

/** Seat configuration; null fields are left unchanged. */
@Data
public class SeatUpdateRequest {
    private ChairType chairType;
    private SeatStatus status;
    private Double seatPriceModifier;
}
