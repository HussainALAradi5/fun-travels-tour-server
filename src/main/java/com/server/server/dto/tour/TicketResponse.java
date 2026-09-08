package com.server.server.dto.tour;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.TicketStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketResponse {
    private Integer id;
    private String ticketNumber;
    private UserSummary customer;
    private TourSummary tour;
    private SeatSummary assignedSeat;
    private BigDecimal basePrice;
    private BigDecimal discountPrice;
    private BigDecimal seatPriceModifier;
    private BigDecimal totalPrice;
    private LocalDateTime bookingDate;
    private boolean isPaid;
    private TicketStatus ticketStatus;
    private GenericStatus approvalStatus;
    private boolean hasMealPlan;
    private String qrCode;
    private String barcode;
    private LocalDateTime createdAt;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class UserSummary {
        private Integer id;
        private String name;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class TourSummary {
        private Integer id;
        private String tourNumber;
        private String title;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class SeatSummary {
        private Integer id;
        private String seatCode;
    }
}
