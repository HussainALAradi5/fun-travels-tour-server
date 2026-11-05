package com.server.server.services.geography;

import com.server.server.exceptions.common.ExternalServiceException;
import com.server.server.dto.geography.CountryCreateRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.server.server.exceptions.common.DuplicateResourceException;
import com.server.server.exceptions.common.ResourceNotFoundException;
import com.server.server.models.geography.Country;
import com.server.server.repositories.geography.CountryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    @Value("${api.restcountries.url}")
    private String restCountriesUrl;

    @Value("${api.restcountries.all.url}")
    private String restCountriesAllUrl;

    @Value("${api.restcountries.api-key:}")
    private String restCountriesApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

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
    public Country createCountry(CountryCreateRequest request) {
        String code = request.countryCode().trim().toUpperCase(java.util.Locale.ROOT);
        if (countryRepository.existsByCountryCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Country code " + code + " already exists.");
        }
        Country country = new Country();
        country.setFamousName(request.famousName().trim());
        country.setOfficialName(request.officialName());
        country.setCountryCode(code);
        country.setDialCode(request.dialCode());
        country.setMobileNumberLength(request.mobileNumberLength());
        country.setFlagPngUrl(request.flagPngUrl());
        country.setFlagSvgUrl(request.flagSvgUrl());
        return countryRepository.save(country);
    }

    @Transactional
    public Country syncFromExternal(String name) {
        String fullUrl = restCountriesUrl + name;
        try {
            List<Map<String, Object>> response = getCountryRecords(fullUrl);
            if (response == null || response.isEmpty()) {
                throw new ResourceNotFoundException("Country not found with name: " + name);
            }
            Map<String, Object> data = response.get(0);
            String code = getNestedString(data, "codes", "alpha_2");

            Optional<Country> existingCountry = countryRepository.findByCountryCodeIgnoreCase(code);
            Country country = existingCountry.orElseGet(Country::new);

            populateCountry(country, data, code);

            return countryRepository.save(country);
        } catch (ResourceNotFoundException | ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Country lookup failed for " + name, e);
        }
    }

    @Transactional
    public Map<String, Integer> syncAllCountries() {
        int addedCount = 0;
        int updatedCount = 0;

        try {
            // v5 limits free-plan list requests to 100 records, so fetch every page.
            int offset = 0;
            while (true) {
                List<Map<String, Object>> response = getCountryRecords(restCountriesAllUrl + "&offset=" + offset);
                for (Map<String, Object> data : response) {
                String code = getNestedString(data, "codes", "alpha_2");

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

                populateCountry(country, data, code);

                countryRepository.save(country);

                if (isNew) {
                    addedCount++;
                } else {
                    updatedCount++;
                }
                }

                if (response.size() < 100) {
                    break;
                }
                offset += response.size();
            }
            Map<String, Integer> result = new HashMap<>();
            result.put("added", addedCount);
            result.put("updated", updatedCount);
            return result;
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Country sync failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getCountryRecords(String url) throws Exception {
        if (restCountriesApiKey == null || restCountriesApiKey.isBlank()) {
            throw new ExternalServiceException("REST_COUNTRIES_API_KEY is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(restCountriesApiKey);
        String responseBody = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
        if (responseBody == null || responseBody.isBlank()) {
            throw new ExternalServiceException("Rest Countries returned an empty response");
        }

        JsonNode responseJson = objectMapper.readTree(responseBody);
        JsonNode records = responseJson.path("data").path("objects");
        if (!records.isArray()) {
            String message = responseJson.path("message").asText(responseJson.toString());
            throw new ExternalServiceException("Rest Countries returned an error response: " + message);
        }
        return objectMapper.readValue(records.toString(), new TypeReference<List<Map<String, Object>>>() {});
    }

    @SuppressWarnings("unchecked")
    private void populateCountry(Country country, Map<String, Object> data, String code) {
        Map<String, Object> names = (Map<String, Object>) data.get("names");
        Map<String, Object> flag = (Map<String, Object>) data.get("flag");
        List<String> callingCodes = (List<String>) data.get("calling_codes");

        country.setFamousName(String.valueOf(names.get("common")));
        country.setOfficialName(String.valueOf(names.get("official")));
        country.setCountryCode(code);
        country.setFlagPngUrl(String.valueOf(flag.get("url_png")));
        country.setFlagSvgUrl(String.valueOf(flag.get("url_svg")));
        country.setDialCode(callingCodes == null || callingCodes.isEmpty() ? null : "+" + callingCodes.get(0));
    }

    @SuppressWarnings("unchecked")
    private String getNestedString(Map<String, Object> data, String objectName, String valueName) {
        Map<String, Object> object = (Map<String, Object>) data.get(objectName);
        return String.valueOf(object.get(valueName));
    }

    @Transactional
    public void deleteCountry(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        if (!countryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Country", id);
        }
        countryRepository.deleteById(id);
    }

}
