package com.server.server.services.agency;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.server.models.City;
import com.server.server.models.Country;
import com.server.server.models.User;
import com.server.server.models.agency.Agency;
import com.server.server.repositories.CityRepository;
import com.server.server.repositories.CountryRepository;
import com.server.server.repositories.UserRepository;
import com.server.server.repositories.agency.AgencyRepository;

@Service
public class AgencyService {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Fetches all agencies from the database.
     */
    public List<Agency> getAllAgencies() {
        return agencyRepository.findAll();
    }

    /**
     * Fetches a single agency by ID.
     */
    public Agency getAgencyById(Integer id) {
        return agencyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agency not found with ID: " + id));
    }

    /**
     * Creates a new agency and assigns an existing user as the Owner.
     * Uses saveAndFlush to prevent Foreign Key constraint violations.
     */
    @Transactional
    public Agency createAgencyFromMap(Map<String, Object> payload) {
        try {
            // 1. Map basic fields from payload to Agency Entity
            Agency agency = objectMapper.convertValue(payload, Agency.class);

            // 2. Handle Country Relationship
            Integer countryId = extractId(payload, "countryId");
            if (countryId != null) {
                Country country = countryRepository.findById(countryId)
                        .orElseThrow(() -> new RuntimeException("Country not found with ID: " + countryId));
                agency.setCountry(country);
            }

            // 3. Handle City Relationship
            Integer cityId = extractId(payload, "cityId");
            if (cityId != null) {
                City city = cityRepository.findById(cityId)
                        .orElseThrow(() -> new RuntimeException("City not found with ID: " + cityId));
                agency.setCity(city);
            }

            // 4. CRITICAL: Save and Flush the Agency first.
            // This forces the DB to generate the ID (e.g., 9) and makes it visible
            // to subsequent statements (User update) in this transaction.
            Agency savedAgency = agencyRepository.saveAndFlush(agency);

            // 5. Link the Agency Owner
            Integer ownerId = extractId(payload, "agencyOwnerId");
            if (ownerId != null) {
                User owner = userRepository.findById(ownerId)
                        .orElseThrow(() -> new RuntimeException("Owner User not found with ID: " + ownerId));

                // Update owner's role and link to the newly created agency
                owner.setAgency(savedAgency);
                // owner.setUserType(UserTypeEnum.OWNER);

                // Save the updated user
                userRepository.save(owner);

                // Set the owner object back to agency for the API response
                savedAgency.setAgencyOwner(owner);
            }

            return savedAgency;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create agency: " + e.getMessage());
        }
    }
public List<User> getEmployeesByAgencyId(Integer agencyId) {
    return userRepository.findByAgencyIdAndIsActiveTrue(agencyId);
}
    /**
     * Helper to safely extract IDs from the payload map
     */
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