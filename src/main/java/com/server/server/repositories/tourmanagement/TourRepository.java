package com.server.server.repositories.tourmanagement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beust.jcommander.internal.Nullable;
import com.server.server.enums.GenericStatus;
import com.server.server.models.tourmanagement.Tour;

import jakarta.persistence.LockModeType;

public interface TourRepository extends JpaRepository<Tour, Integer>, JpaSpecificationExecutor<Tour> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tour t WHERE t.id = :id")
    Optional<Tour> findByIdWithLock(@Param("id") Integer id);

    List<Tour> findByStatus(GenericStatus status);

    List<Tour> findByAgency_Id(Integer agencyId);

    List<Tour> findByTransportation_Id(Integer transportationId);

    @EntityGraph(attributePaths = {
            "startCountry",
            "endCountry",
            "startCity",
            "endCity",
            "transportation",
            "agency",
            "destinationCountries",
            "availableMeals",
            "tickets", // Load the tickets
            "tickets.customer" // Load the customer inside each ticket

    })
    @Query("SELECT t FROM Tour t WHERE t.id = :id")
    Optional<Tour> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT t FROM Tour t LEFT JOIN FETCH t.destinationCountries WHERE t.id = :id")
    Optional<Tour> findWithDestinations(@Param("id") Integer id);

    @Query("SELECT t FROM Tour t LEFT JOIN FETCH t.tickets WHERE t.id = :id")
    Optional<Tour> findWithTickets(@Param("id") Integer id);

    List<Tour> findByStartDateAndStatus(LocalDate startDate, GenericStatus status);

    @Query("SELECT DISTINCT t FROM Tour t " +
            "LEFT JOIN FETCH t.startCountry " +
            "LEFT JOIN FETCH t.endCountry " +
            "LEFT JOIN FETCH t.startCity " +
            "LEFT JOIN FETCH t.endCity " +
            "LEFT JOIN FETCH t.transportation " +
            "LEFT JOIN FETCH t.destinationCountries")
    List<Tour> findAllWithDetails();

    @Override
    @EntityGraph(attributePaths = {
            "startCountry", "endCountry", "startCity", "endCity",
            "transportation", "destinationCountries"
    })
    List<Tour> findAll(@Nullable Specification<Tour> spec, Sort sort);

    @Query("SELECT t FROM Tour t " +
            "LEFT JOIN FETCH t.startCountry " +
            "LEFT JOIN FETCH t.endCountry " +
            "LEFT JOIN FETCH t.startCity " +
            "LEFT JOIN FETCH t.endCity " +
            "WHERE (:startCountryId IS NULL OR t.startCountry.id = :startCountryId) " +
            "AND (:endCountryId IS NULL OR t.endCountry.id = :endCountryId) " +
            "AND (CAST(:start AS date) IS NULL OR t.startDate >= :start) " +
            "AND (CAST(:end AS date) IS NULL OR t.endDate <= :end) " +
            "AND t.status = 'ACTIVE' " +
            "ORDER BY t.startDate ASC")
    List<Tour> findToursForCatalog(
            @Param("startCountryId") Integer startCountryId,
            @Param("endCountryId") Integer endCountryId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    List<Tour> findByStartDateBeforeAndStatus(LocalDate date, GenericStatus status);

    List<Tour> findByEndDateBeforeAndStatusIn(LocalDate date, List<GenericStatus> statuses);

}
