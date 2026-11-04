package com.server.server.repositories.tourmanagement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.models.tourmanagement.Seat;
public interface SeatRepository extends JpaRepository<Seat, Integer>, JpaSpecificationExecutor<Seat> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithLock(@Param("id") Integer id);

    List<Seat> findByTransportation_IdAndStatusNotOrderByIdAsc(Integer transportationId, SeatStatus status);

    List<Seat> findByTransportation_IdOrderByIdAsc(Integer transportationId);
}
