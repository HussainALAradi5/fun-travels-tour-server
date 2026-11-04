package com.server.server.dto.tourmanagement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.server.server.dto.user.UserSummary;
import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.models.tourmanagement.Ticket;
/** @param qrCode PNG data URL; only present once the ticket is paid */
public record TicketResponse(
        Integer id,
        String ticketNumber,
        UserSummary customer,
        TourSummary tour,
        SeatSummary assignedSeat,
        BigDecimal basePrice,
        BigDecimal discountPrice,
        BigDecimal seatPriceModifier,
        BigDecimal totalPrice,
        LocalDateTime bookingDate,
        boolean paid,
        TicketStatus ticketStatus,
        GenericStatus approvalStatus,
        boolean hasMealPlan,
        String qrCode,
        String barcode,
        LocalDateTime createdAt) {

    public static TicketResponse from(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        return new TicketResponse(ticket.getId(), ticket.getTicketNumber(), UserSummary.from(ticket.getCustomer()),
                TourSummary.from(ticket.getTour()), SeatSummary.from(ticket.getAssignedSeat()), ticket.getBasePrice(),
                ticket.getDiscountPrice(), ticket.getSeatPriceModifier(), ticket.getTotalPrice(), ticket.getBookingDate(),
                ticket.isPaid(), ticket.getTicketStatus(), ticket.getApprovalStatus(), ticket.isHasMealPlan(),
                ticket.getQrCode(), ticket.getBarcode(), ticket.getCreatedAt());
    }
}
