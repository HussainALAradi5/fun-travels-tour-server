package com.server.server.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.server.models.City;
import com.server.server.repositories.CityRepository;

@Service
public class CityService {

    @Autowired
    private CityRepository cityRepository;

    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    public List<City> getCitiesByCountry(Integer countryId) {
        return cityRepository.findByCountryId(countryId);
    }

    public City createCity(City city) {
        // 1. Check if country info is missing
        if (city.getCountry() == null || city.getCountry().getId() == null) {
            throw new RuntimeException("Valid Country is required to add a city.");
        }

        // 2. The Duplicate Check
        boolean exists = cityRepository.existsByNameIgnoreCaseAndCountryId(
                city.getName().trim(), // trim to avoid "City " vs "City"
                city.getCountry().getId());

        if (exists) {
            // This message is what we want to see in the frontend alert
            throw new RuntimeException("City '" + city.getName() + "' already exists in this country.");
        }

        return cityRepository.save(city);
    }

    public void deleteCity(Integer id) {
        if (!cityRepository.existsById(id)) {
            throw new RuntimeException("City not found with ID: " + id);
        }
        cityRepository.deleteById(id);
    }
}