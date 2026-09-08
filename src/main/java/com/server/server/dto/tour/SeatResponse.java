package com.server.server.dto.tour;

import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatResponse {
    private Integer id;
    private String seatCode;
    private ChairType chairType;
    private SeatStatus status;
    private Double seatPriceModifier;
}
