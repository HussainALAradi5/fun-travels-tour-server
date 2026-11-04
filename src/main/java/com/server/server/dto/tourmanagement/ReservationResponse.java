package com.server.server.dto.tourmanagement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.server.server.dto.user.UserSummary;
import com.server.server.enums.common.GenericStatus;
import com.server.server.models.tourmanagement.TourReservation;
public record ReservationResponse(
        Integer id,
        String reservationNumber,
        TourSummary tour,
        UserSummary user,
        Integer requestedSlots,
        BigDecimal totalPrice,
        GenericStatus status,
        LocalDateTime bookingDate,
        LocalDateTime holdExpiresAt) {

    public static ReservationResponse from(TourReservation reservation) {
        if (reservation == null) {
            return null;
        }
        return new ReservationResponse(reservation.getId(), reservation.getReservationNumber(),
                TourSummary.from(reservation.getTour()), UserSummary.from(reservation.getUser()),
                reservation.getRequestedSlots(), reservation.getTotalPrice(), reservation.getStatus(),
                reservation.getBookingDate(), reservation.getHoldExpiresAt());
    }
}
