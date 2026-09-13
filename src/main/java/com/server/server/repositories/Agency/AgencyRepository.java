package com.server.server.repositories.agency;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.server.server.models.agency.Agency;

public interface AgencyRepository extends JpaRepository<Agency, Integer> {

    List<Agency> findByIsActiveTrue();

    List<Agency> findByAgencyOwnerIdAndIsActiveTrue(Integer ownerId);

    boolean existsByAgencyNameIgnoreCaseAndCountryId(String agencyName, Integer countryId);

    Page<Agency> findByAgencyNameContainingIgnoreCase(String agencyName, Pageable pageable);
}
