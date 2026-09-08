package com.server.server.dto.tour;

import java.util.List;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.TransportationStatus;
import com.server.server.enums.tourmanagement.TransportationType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransportationResponse {
    private Integer id;
    private String transportationNumber;
    private String code;
    private TransportationType type;
    private String providerName;
    private GenericStatus status;
    private TransportationStatus unitStatus;
    private Integer totalCapacity;
    private Integer remainingSeats;
    private Integer calculatedAvailable;
    private List<SeatSummary> seats;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class SeatSummary {
        private Integer id;
        private String seatCode;
        private String chairType;
        private String status;
    }
}
