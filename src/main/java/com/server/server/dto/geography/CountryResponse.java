package com.server.server.dto.geography;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CountryResponse {
    private Integer id;
    private String famousName;
    private String officialName;
    private String countryCode;
    private String flagPngUrl;
    private String flagSvgUrl;
    private String dialCode;
    private Integer mobileNumberLength;
}
