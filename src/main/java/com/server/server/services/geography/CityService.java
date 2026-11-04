package com.server.server.services.geography;

import com.server.server.repositories.geography.CountryRepository;

import com.server.server.models.geography.Country;

import com.server.server.dto.geography.CityCreateRequest;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.exceptions.DuplicateResourceException;
import com.server.server.exceptions.ResourceNotFoundException;
import com.server.server.models.geography.City;
import com.server.server.repositories.geography.CityRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;

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
    public City createCity(CityCreateRequest request) {
        Country country = countryRepository.findById(request.country().id())
                .orElseThrow(() -> new ResourceNotFoundException("Country", request.country().id()));
        String name = request.name().trim();
        if (cityRepository.existsByNameIgnoreCaseAndCountryId(name, country.getId())) {
            throw new DuplicateResourceException("City '" + name + "' already exists in this country.");
        }
        City city = new City();
        city.setName(name);
        city.setCountry(country);
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
