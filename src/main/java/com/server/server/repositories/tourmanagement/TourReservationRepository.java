package com.server.server.repositories.tourmanagement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.server.server.enums.GenericStatus;
import com.server.server.models.tourmanagement.TourReservation;

public interface TourReservationRepository
              extends JpaRepository<TourReservation, Integer>, JpaSpecificationExecutor<TourReservation> {
       List<TourReservation> findByStatus(GenericStatus status);

       Optional<TourReservation> findByReservationNumber(String reservationNumber);

       List<TourReservation> findByUser_Id(Integer userId); // Fixed mapping

       @Query("SELECT COUNT(r) > 0 FROM TourReservation r " +
                     "WHERE r.user.id = :userId " +
                     "AND r.status IN (com.server.server.enums.GenericStatus.PENDING, " +
                     "com.server.server.enums.GenericStatus.APPROVED, " +
                     "com.server.server.enums.GenericStatus.CONFIRMED) " +
                     "AND r.tour.startDate <= :newEndDate " +
                     "AND COALESCE(r.tour.endDate, r.tour.startDate) >= :newStartDate")
       boolean hasOverlappingReservations(
                     @Param("userId") Integer userId,
                     @Param("newStartDate") LocalDate newStartDate,
                     @Param("newEndDate") LocalDate newEndDate);

       @Query("SELECT r FROM TourReservation r JOIN r.tour t WHERE t.startDate = :date AND r.status = :status")
       List<TourReservation> findAllByTourStartDateAndStatus(LocalDate date, GenericStatus status);
}
