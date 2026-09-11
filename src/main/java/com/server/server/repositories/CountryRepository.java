package com.server.server.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.server.models.Country;

public interface CountryRepository extends JpaRepository<Country,Integer> {
    boolean existsByCountryCodeIgnoreCase(String countryCode);

    Optional<Country> findByCountryCodeIgnoreCase(String countryCode);
}
