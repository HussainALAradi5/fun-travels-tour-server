package com.server.server.dto.tourmanagement;

import com.server.server.models.tourmanagement.TourReservation;
/** A reservation by id and number. */
public record ReservationSummary(Integer id, String reservationNumber) {

    public static ReservationSummary from(TourReservation source) {
        return source == null ? null : new ReservationSummary(source.getId(), source.getReservationNumber());
    }
}
