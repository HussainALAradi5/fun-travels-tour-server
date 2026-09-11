package com.server.server.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.server.server.exceptions.DuplicateResourceException;
import com.server.server.exceptions.ResourceNotFoundException;
import com.server.server.models.Country;
import com.server.server.repositories.CountryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    @Value("${api.restcountries.url}")
    private String restCountriesUrl;

    private final RestTemplate restTemplate;

    @Transactional(readOnly = true)
    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Country getCountryById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return countryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country", id));
    }

    @Transactional
    public Country createCountry(Country country) {
        if (countryRepository.existsByCountryCodeIgnoreCase(country.getCountryCode())) {
            throw new DuplicateResourceException("Country code " + country.getCountryCode() + " already exists.");
        }
        return countryRepository.save(country);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Country syncFromExternal(String name) {
        String fullUrl = restCountriesUrl + name;
        try {
            List<Map<String, Object>> response = restTemplate.getForObject(fullUrl, List.class);
            if (response == null || response.isEmpty()) {
                throw new ResourceNotFoundException("Country not found with name: " + name);
            }
            Map<String, Object> data = response.get(0);
            String code = String.valueOf(data.get("cca2"));

            Optional<Country> existingCountry = countryRepository.findByCountryCodeIgnoreCase(code);
            Country country = existingCountry.orElseGet(Country::new);

            Map<String, Object> nameObj = (Map<String, Object>) data.get("name");
            Map<String, Object> flagsObj = (Map<String, Object>) data.get("flags");
            Map<String, Object> iddObj = (Map<String, Object>) data.get("idd");

            country.setFamousName(String.valueOf(nameObj.get("common")));
            country.setOfficialName(String.valueOf(nameObj.get("official")));
            country.setCountryCode(code);
            country.setFlagPngUrl(String.valueOf(flagsObj.get("png")));
            country.setFlagSvgUrl(String.valueOf(flagsObj.get("svg")));
            country.setDialCode(extractDialCode(iddObj));

            return countryRepository.save(country);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("External Sync Failed: " + e.getMessage());
        }
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Integer> syncAllCountries() {
        String allUrl = "https://restcountries.com/v3.1/all?fields=name,cca2,flags,idd";
        int addedCount = 0;
        int updatedCount = 0;

        try {
            List<Map<String, Object>> response = restTemplate.getForObject(allUrl, List.class);
            if (response != null) {
                for (Map<String, Object> data : response) {
                    String code = String.valueOf(data.get("cca2"));

                    Optional<Country> existingOpt = countryRepository.findByCountryCodeIgnoreCase(code);
                    Country country;
                    boolean isNew = false;

                    if (existingOpt.isPresent()) {
                        country = existingOpt.get();
                    } else {
                        country = new Country();
                        country.setCountryCode(code);
                        isNew = true;
                    }

                    Map<String, Object> nameObj = (Map<String, Object>) data.get("name");
                    Map<String, Object> flagsObj = (Map<String, Object>) data.get("flags");
                    Map<String, Object> iddObj = (Map<String, Object>) data.get("idd");

                    country.setFamousName(String.valueOf(nameObj.get("common")));
                    country.setOfficialName(String.valueOf(nameObj.get("official")));
                    country.setFlagPngUrl(String.valueOf(flagsObj.get("png")));
                    country.setDialCode(extractDialCode(iddObj));

                    countryRepository.save(country);

                    if (isNew) {
                        addedCount++;
                    } else {
                        updatedCount++;
                    }
                }
            }
            Map<String, Integer> result = new HashMap<>();
            result.put("added", addedCount);
            result.put("updated", updatedCount);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("API Sync Failed: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteCountry(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        if (!countryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Country", id);
        }
        countryRepository.deleteById(id);
    }

    @SuppressWarnings("unchecked")
    private String extractDialCode(Map<String, Object> iddObj) {
        if (iddObj != null && iddObj.get("root") != null) {
            String root = String.valueOf(iddObj.get("root"));
            List<String> suffixes = (List<String>) iddObj.get("suffixes");
            if (suffixes != null && suffixes.size() == 1) {
                return root + suffixes.get(0);
            }
            return root;
        }
        return null;
    }
}
