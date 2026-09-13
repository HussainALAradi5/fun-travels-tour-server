package com.server.server.dto.geography;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CityResponse {
    private Integer id;
    private String name;
    private CountrySummary country;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class CountrySummary {
        private Integer id;
        private String famousName;
    }
}
