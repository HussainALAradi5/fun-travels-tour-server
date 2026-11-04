package com.server.server.dto.tourmanagement;

import java.time.LocalDate;

import com.server.server.models.tourmanagement.Tour;
/** A tour as tickets and reservations show it, with its travel dates. */
public record TourSummary(Integer id, String tourNumber, String title, LocalDate startDate, LocalDate endDate) {

    public static TourSummary from(Tour source) {
        return source == null ? null : new TourSummary(source.getId(), source.getTourNumber(), source.getTitle(), source.getStartDate(), source.getEndDate());
    }
}
