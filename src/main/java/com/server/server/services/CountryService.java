package com.server.server.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.server.server.models.Country;
import com.server.server.repositories.CountryRepository;

@Service
public class CountryService {

    @Autowired
    private CountryRepository countryRepository;

    @Value("${api.restcountries.url}")
    private String restCountriesUrl;

    @Value("${api.restcountries.all.url}")
    private String allCountriesUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    public Country getCountryById(Integer id) {
        return countryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Country not found with ID: " + id));
    }

    public Country createCountry(Country country, String userType) {
        validateAdmin(userType);
        if (countryRepository.existsByCountryCodeIgnoreCase(country.getCountryCode())) {
            throw new RuntimeException("Country code " + country.getCountryCode() + " already exists.");
        }
        return countryRepository.save(country);
    }

    @SuppressWarnings("unchecked")
    public Country syncFromExternal(String name, String userType) {
        validateAdmin(userType);
        String fullUrl = restCountriesUrl + name;
        try {
            List<Map<String, Object>> response = restTemplate.getForObject(fullUrl, List.class);
            if (response == null || response.isEmpty()) {
                throw new RuntimeException("No country found with name: " + name);
            }
            Map<String, Object> data = response.get(0);
            String code = String.valueOf(data.get("cca2"));

            // Check if it exists to UPDATE rather than throw an error
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
        } catch (Exception e) {
            throw new RuntimeException("External Sync Failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> syncAllCountries(String userType) {
        validateAdmin(userType);
        String allUrl = "https://restcountries.com/v3.1/all?fields=name,cca2,flags,idd";
        int addedCount = 0;
        int updatedCount = 0; // Let's track updates too!

        try {
            List<Map<String, Object>> response = restTemplate.getForObject(allUrl, List.class);
            if (response != null) {
                for (Map<String, Object> data : response) {
                    String code = String.valueOf(data.get("cca2"));

                    // Fetch existing or create new
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

    public void deleteCountry(Integer id, String userType) {
        validateAdmin(userType);
        if (!countryRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete: Country not found with ID: " + id);
        }
        countryRepository.deleteById(id);
    }

    private void validateAdmin(String userType) {
        if (!"ADMIN".equalsIgnoreCase(userType)) {
            throw new RuntimeException("Access Denied: Only ADMINs can perform this action.");
        }
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