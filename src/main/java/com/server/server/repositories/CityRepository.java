package com.server.server.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.server.server.models.City;

public interface CityRepository extends JpaRepository<City, Integer> {

    @Override
    @EntityGraph(attributePaths = "country")
    List<City> findAll();

    @Query("select city from City city join fetch city.country where city.country.id = :countryId")
    List<City> findByCountryId(@Param("countryId") Integer countryId);

    boolean existsByNameIgnoreCaseAndCountryId(String name, Integer countryId);

    List<City> findByNameContainingIgnoreCase(String name);
}
