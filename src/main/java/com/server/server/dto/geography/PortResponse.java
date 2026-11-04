package com.server.server.dto.geography;

import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.geography.PortType;
import com.server.server.models.geography.Port;
public record PortResponse(
        Integer id,
        String portName,
        String portCode,
        PortType portType,
        CitySummary city,
        CountrySummary country,
        Double latitude,
        Double longitude,
        GenericStatus status) {

    public static PortResponse from(Port port) {
        if (port == null) {
            return null;
        }
        return new PortResponse(port.getId(), port.getPortName(), port.getPortCode(), port.getPortType(),
                CitySummary.from(port.getCity()), CountrySummary.from(port.getCountry()), port.getLatitude(),
                port.getLongitude(), port.getStatus());
    }
}
