package com.server.server.repositories.tourmanagement;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.server.server.models.tourmanagement.Transportation;
public interface TransportationRepository extends JpaRepository<Transportation, Integer>, JpaSpecificationExecutor<Transportation> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transportation t WHERE t.id = :id")
    Optional<Transportation> findByIdWithLock(@Param("id") Integer id);
    @EntityGraph(attributePaths = { "seats", "agency", "agencyBranch" })
    Optional<Transportation> findWithSeatsById(Integer id);

    boolean existsByCode(String code);
    boolean existsByProviderNameAndTransportationNumber(String providerName, String transportationNumber);
    boolean existsByProviderNameAndTransportationNumberAndIdNot(String providerName, String transportationNumber, Integer id);
}
