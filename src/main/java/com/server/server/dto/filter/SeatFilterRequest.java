package com.server.server.dto.filter;

import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SeatFilterRequest extends GenericFilterRequest {
    private Integer transportId;
    private SeatStatus status;
    private ChairType chairType;
    private String keyword;
}
