package com.server.server.dto.tour;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.server.server.enums.GenericStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TourResponse {
    private Integer id;
    private String tourNumber;
    private String title;
    private String description;
    private Double basePrice;
    private Double discountPrice;
    private Double totalPrice;
    private Integer numberOfDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxCapacity;
    private Integer availableSlots;
    private GenericStatus status;
    private boolean hasTransportation;
    private CountrySummary startCountry;
    private CountrySummary endCountry;
    private CitySummary startCity;
    private CitySummary endCity;
    private AgencySummary agency;
    private BranchSummary agencyBranch;
    private TransportationSummary transportation;
    private UserSummary createdBy;
    private LocalDateTime createdAt;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class CountrySummary {
        private Integer id;
        private String famousName;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class CitySummary {
        private Integer id;
        private String name;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class AgencySummary {
        private Integer id;
        private String agencyName;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class BranchSummary {
        private Integer id;
        private String branchName;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class TransportationSummary {
        private Integer id;
        private String transportationNumber;
        private String type;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class UserSummary {
        private Integer id;
        private String name;
    }
}
