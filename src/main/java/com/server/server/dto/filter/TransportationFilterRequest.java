package com.server.server.dto.filter;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.TransportationStatus;
import com.server.server.enums.tourmanagement.TransportationType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TransportationFilterRequest extends GenericFilterRequest {
    private TransportationType type;
    private GenericStatus status;
    private TransportationStatus unitStatus;
    private String keyword;
}
