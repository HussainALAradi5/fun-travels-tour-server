package com.server.server.dto.tour;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.server.server.enums.GenericStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationResponse {
    private Integer id;
    private String reservationNumber;
    private TourSummary tour;
    private UserSummary user;
    private Integer requestedSlots;
    private BigDecimal totalPrice;
    private GenericStatus status;
    private LocalDateTime bookingDate;
    private LocalDateTime holdExpiresAt;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class TourSummary {
        private Integer id;
        private String tourNumber;
        private String title;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class UserSummary {
        private Integer id;
        private String name;
    }
}
