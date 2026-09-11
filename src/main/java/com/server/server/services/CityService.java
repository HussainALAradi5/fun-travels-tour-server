package com.server.server.services;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.exceptions.DuplicateResourceException;
import com.server.server.exceptions.ResourceNotFoundException;
import com.server.server.models.City;
import com.server.server.repositories.CityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    @Transactional(readOnly = true)
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<City> getCitiesByCountry(@NonNull Integer countryId) {
        Objects.requireNonNull(countryId, "countryId must not be null");
        return cityRepository.findByCountryId(countryId);
    }

    @Transactional
    public City createCity(City city) {
        if (city.getCountry() == null || city.getCountry().getId() == null) {
            throw new IllegalArgumentException("Valid Country is required to add a city.");
        }

        boolean exists = cityRepository.existsByNameIgnoreCaseAndCountryId(
                city.getName().trim(), city.getCountry().getId());

        if (exists) {
            throw new DuplicateResourceException("City '" + city.getName() + "' already exists in this country.");
        }

        return cityRepository.save(city);
    }

    @Transactional
    public void deleteCity(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        if (!cityRepository.existsById(id)) {
            throw new ResourceNotFoundException("City", id);
        }
        cityRepository.deleteById(id);
    }
}
