package com.server.server.repositories.tourmanagement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.TransportationStatus;
import com.server.server.enums.tourmanagement.TransportationType;
import com.server.server.models.tourmanagement.Transportation;

public interface TransportationRepository extends JpaRepository<Transportation, Integer>, JpaSpecificationExecutor<Transportation> {
    boolean existsByCode(String code);
    boolean existsByProviderNameAndCode(String providerName, String code);
    boolean existsByProviderNameAndTransportationNumber(String providerName, String transportationNumber);
    boolean existsByProviderNameAndCodeAndIdNot(String providerName, String code, Integer id);
    boolean existsByProviderNameAndTransportationNumberAndIdNot(String providerName, String transportationNumber, Integer id);
    List<Transportation> findByStatus(GenericStatus status);

    @Query("SELECT t FROM Transportation t WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "  LOWER(t.providerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(t.transportationNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           ") " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:unitStatus IS NULL OR t.unitStatus = :unitStatus)")
    List<Transportation> filterAndSearch(
            @Param("keyword") String keyword,
            @Param("type") TransportationType type,
            @Param("status") GenericStatus status,
            @Param("unitStatus") TransportationStatus unitStatus
    );
}
