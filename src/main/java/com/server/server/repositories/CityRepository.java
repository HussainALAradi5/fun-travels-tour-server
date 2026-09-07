package com.server.server.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.server.server.models.City;

@Repository
public interface CityRepository extends JpaRepository<City, Integer> {
    List<City> findByCountryId(Integer countryId);

    boolean existsByNameIgnoreCaseAndCountryId(String name, Integer countryId);

    List<City> findByNameContainingIgnoreCase(String name);
}