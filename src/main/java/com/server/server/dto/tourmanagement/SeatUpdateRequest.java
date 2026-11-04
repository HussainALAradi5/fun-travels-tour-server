package com.server.server.dto.tourmanagement;

import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;

/** Seat configuration; null fields are left unchanged. */
public record SeatUpdateRequest(
        ChairType chairType,
        SeatStatus status,
        Double seatPriceModifier) {
}
