package com.server.server.dto.tourmanagement;

import com.server.server.dto.common.GenericFilterRequest;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.server.server.enums.common.GenericStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TourFilterRequest extends GenericFilterRequest {
    private GenericStatus status;
    private Integer minSlots;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private Long agencyId;
    private Long branchId;
    private Double minPrice;
    private Double maxPrice;
    private Integer countryId;
    private Integer cityId;
    private Integer createdById;
}
