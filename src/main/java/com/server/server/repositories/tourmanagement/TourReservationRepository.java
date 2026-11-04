package com.server.server.repositories.tourmanagement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import org.springframework.data.repository.query.Param;

import com.server.server.enums.common.GenericStatus;
import com.server.server.models.tourmanagement.TourReservation;
public interface TourReservationRepository
              extends JpaRepository<TourReservation, Integer>, JpaSpecificationExecutor<TourReservation> {


       @Lock(LockModeType.PESSIMISTIC_WRITE)
       @Query("SELECT r FROM TourReservation r WHERE r.id = :id")
       Optional<TourReservation> findByIdWithLock(@Param("id") Integer id);

       List<TourReservation> findByStatusAndHoldExpiresAtBefore(GenericStatus status, LocalDateTime expiresAt);

       List<TourReservation> findByTour_IdAndStatusIn(Integer tourId, List<GenericStatus> statuses);
 // Fixed mapping

       @Query("SELECT COUNT(r) > 0 FROM TourReservation r " +
                     "WHERE r.user.id = :userId " +
                     "AND r.status IN (com.server.server.enums.common.GenericStatus.PENDING, " +
                     "com.server.server.enums.common.GenericStatus.APPROVED, " +
                     "com.server.server.enums.common.GenericStatus.CONFIRMED) " +
                     "AND r.tour.startDate <= :newEndDate " +
                     "AND COALESCE(r.tour.endDate, r.tour.startDate) >= :newStartDate")
       boolean hasOverlappingReservations(
                     @Param("userId") Integer userId,
                     @Param("newStartDate") LocalDate newStartDate,
                     @Param("newEndDate") LocalDate newEndDate);
}
