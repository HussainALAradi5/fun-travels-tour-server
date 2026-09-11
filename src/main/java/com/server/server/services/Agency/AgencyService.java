package com.server.server.services.agency;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.server.exceptions.ResourceNotFoundException;
import com.server.server.models.City;
import com.server.server.models.Country;
import com.server.server.models.User;
import com.server.server.models.agency.Agency;
import com.server.server.repositories.CityRepository;
import com.server.server.repositories.CountryRepository;
import com.server.server.repositories.UserRepository;
import com.server.server.repositories.agency.AgencyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgencyService {

    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<Agency> getAllAgencies() {
        return agencyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Agency getAgencyById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return agencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agency", id));
    }

    @Transactional
    public Agency createAgencyFromMap(Map<String, Object> payload) {
        Agency agency = objectMapper.convertValue(payload, Agency.class);

        Integer countryId = extractId(payload, "countryId");
        if (countryId != null) {
            Country country = countryRepository.findById(countryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Country", countryId));
            agency.setCountry(country);
        }

        Integer cityId = extractId(payload, "cityId");
        if (cityId != null) {
            City city = cityRepository.findById(cityId)
                    .orElseThrow(() -> new ResourceNotFoundException("City", cityId));
            agency.setCity(city);
        }

        Agency savedAgency = agencyRepository.saveAndFlush(agency);

        Integer ownerId = extractId(payload, "agencyOwnerId");
        if (ownerId != null) {
            User owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

            owner.setAgency(savedAgency);
            userRepository.save(owner);
            savedAgency.setAgencyOwner(owner);
        }

        return savedAgency;
    }

    @Transactional(readOnly = true)
    public List<User> getEmployeesByAgencyId(@NonNull Integer agencyId) {
        Objects.requireNonNull(agencyId, "agencyId must not be null");
        return userRepository.findByAgencyIdAndIsActiveTrue(agencyId);
    }

    private Integer extractId(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null)
            return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
