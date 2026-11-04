package com.server.server.dto.tourmanagement;

import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.models.tourmanagement.Seat;
/** A seat with its chair type and fleet status (AVAILABLE or MAINTENANCE). */
public record SeatSummary(Integer id, String seatCode, ChairType chairType, SeatStatus status) {

    public static SeatSummary from(Seat source) {
        return source == null ? null : new SeatSummary(source.getId(), source.getSeatCode(), source.getChairType(), source.getStatus());
    }
}
