package com.server.server.dto.geography;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.PortType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortResponse {
    private Integer id;
    private String portName;
    private String portCode;
    private PortType portType;
    private CitySummary city;
    private CountrySummary country;
    private Double latitude;
    private Double longitude;
    private GenericStatus status;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class CitySummary {
        private Integer id;
        private String name;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class CountrySummary {
        private Integer id;
        private String famousName;
    }
}
