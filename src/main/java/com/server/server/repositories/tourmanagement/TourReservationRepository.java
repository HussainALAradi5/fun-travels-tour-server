package com.server.server.repositories.tourmanagement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.server.server.enums.GenericStatus;
import com.server.server.models.tourmanagement.TourReservation;

@Repository
public interface TourReservationRepository
              extends JpaRepository<TourReservation, Integer>, JpaSpecificationExecutor<TourReservation> {
       List<TourReservation> findByStatus(GenericStatus status);

       Optional<TourReservation> findByReservationNumber(String reservationNumber);

       List<TourReservation> findByUser_Id(Integer userId); // Fixed mapping

       @Query("SELECT COUNT(r) > 0 FROM TourReservation r " +
                     "WHERE r.user.id = :userId " +
                     "AND r.status != com.server.server.enums.GenericStatus.CANCELLED " +
                     "AND r.tour.startDate <= :newEndDate " +
                     "AND r.tour.endDate >= :newStartDate")
       boolean hasOverlappingReservations(
                     @Param("userId") Integer userId,
                     @Param("newStartDate") LocalDate newStartDate,
                     @Param("newEndDate") LocalDate newEndDate);

       @Query("SELECT r FROM TourReservation r JOIN r.tour t WHERE t.startDate = :date AND r.status = :status")
       List<TourReservation> findAllByTourStartDateAndStatus(LocalDate date, GenericStatus status);
}
