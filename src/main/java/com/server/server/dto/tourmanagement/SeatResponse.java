package com.server.server.dto.tourmanagement;

import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.models.tourmanagement.Seat;
public record SeatResponse(Integer id, String seatCode, ChairType chairType, SeatStatus status, Double seatPriceModifier) {

    public static SeatResponse from(Seat seat) {
        return seat == null ? null
                : new SeatResponse(seat.getId(), seat.getSeatCode(), seat.getChairType(), seat.getStatus(),
                        seat.getSeatPriceModifier());
    }

    /** The same seat reported with another status, such as BOOKED for one tour. */
    public SeatResponse withStatus(SeatStatus newStatus) {
        return new SeatResponse(id, seatCode, chairType, newStatus, seatPriceModifier);
    }
}
