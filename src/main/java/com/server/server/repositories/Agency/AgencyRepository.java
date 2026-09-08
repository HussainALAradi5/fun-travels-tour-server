package com.server.server.repositories.agency;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.server.server.models.Agency.Agency;


@Repository
public interface AgencyRepository extends JpaRepository<Agency, Integer> {

    List<Agency> findByIsActiveTrue();

    List<Agency> findByAgencyOwnerIdAndIsActiveTrue(Integer ownerId);

    boolean existsByAgencyNameIgnoreCaseAndCountryId(String agencyName, Integer countryId);
}
