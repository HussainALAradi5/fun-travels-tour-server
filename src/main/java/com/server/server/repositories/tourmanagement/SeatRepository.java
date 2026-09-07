package com.server.server.repositories.tourmanagement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.models.tourmanagement.Seat;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer>, JpaSpecificationExecutor<Seat> {

    @Query("SELECT s FROM Seat s WHERE s.transportation.id = :transportId " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(s.seatCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR s.status = :status) " +
            "AND (:chairType IS NULL OR s.chairType = :chairType)")
    List<Seat> filterAndSearch(
            @Param("transportId") Integer transportId,
            @Param("keyword") String keyword,
            @Param("status") SeatStatus status,
            @Param("chairType") ChairType chairType);
}