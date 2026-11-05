package com.server.server.services.geography;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.server.constants.geography.MobileNumberLengths;
import com.server.server.exceptions.common.ExternalServiceException;
import com.server.server.models.geography.Country;
import com.server.server.repositories.geography.CountryRepository;
import com.server.server.utilities.geography.FlagUrls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads every country (names, ISO code, dial code, flags) from a free public dataset that needs no API key:
 * the {@code world-countries} package on jsDelivr ({@code app.countries.directory-url}). Existing countries keep
 * the values they have; only empty fields are filled.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CountryDirectoryService {

    /** Codes shared by many countries, where the area suffixes are not part of the country code. */
    private static final Set<String> SHARED_ROOTS = Set.of("+1", "+7");

    private final CountryRepository countryRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.countries.directory-url:https://cdn.jsdelivr.net/npm/world-countries@5.1.0/countries.json}")
    private String directoryUrl;

    @Transactional
    public Map<String, Integer> importAll() {
        JsonNode records = fetch();
        Map<String, Country> existing = new HashMap<>();
        countryRepository.findAll().forEach(c -> {
            if (c.getCountryCode() != null) {
                existing.put(c.getCountryCode().toUpperCase(Locale.ROOT), c);
            }
        });
        int added = 0;
        int updated = 0;
        for (JsonNode record : records) {
            String code = record.path("cca2").asText("").toUpperCase(Locale.ROOT);
            if (!code.matches("[A-Z]{2}")) {
                continue;
            }
            Country country = existing.get(code);
            boolean isNew = country == null;
            if (isNew) {
                country = new Country();
                country.setCountryCode(code);
            }
            if (fill(country, record, code) || isNew) {
                countryRepository.save(country);
                if (isNew) {
                    added++;
                } else {
                    updated++;
                }
            }
        }
        log.info("Country directory import: {} added, {} updated", added, updated);
        return Map.of("added", added, "updated", updated);
    }

    public long countryCount() {
        return countryRepository.count();
    }

    /** Dial code from the dataset's root and suffixes: +973 for Bahrain, +1 for the shared North American code. */
    static String dialCode(JsonNode idd) {
        String root = idd.path("root").asText("");
        if (root.isBlank()) {
            return null;
        }
        List<String> suffixes = new ArrayList<>();
        idd.path("suffixes").forEach(s -> suffixes.add(s.asText()));
        if (suffixes.isEmpty() || SHARED_ROOTS.contains(root) && suffixes.size() > 1) {
            return root;
        }
        return root + suffixes.stream().min(Comparator.comparingInt(String::length)).orElse("");
    }

    private boolean fill(Country country, JsonNode record, String code) {
        boolean changed = false;
        changed |= set(country.getFamousName(), record.path("name").path("common").asText(null), country::setFamousName);
        changed |= set(country.getOfficialName(), record.path("name").path("official").asText(null), country::setOfficialName);
        changed |= set(country.getDialCode(), dialCode(record.path("idd")), country::setDialCode);
        changed |= set(country.getFlagPngUrl(), FlagUrls.png(code), country::setFlagPngUrl);
        changed |= set(country.getFlagSvgUrl(), FlagUrls.svg(code), country::setFlagSvgUrl);
        Integer length = MobileNumberLengths.BY_COUNTRY_CODE.get(code);
        if (country.getMobileNumberLength() == null && length != null) {
            country.setMobileNumberLength(length);
            changed = true;
        }
        return changed;
    }

    private static boolean set(String current, String value, Consumer<String> setter) {
        if (value == null || value.isBlank() || current != null && !current.isBlank() && !"null".equals(current)) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    private JsonNode fetch() {
        try {
            String body = restTemplate.getForObject(directoryUrl, String.class);
            JsonNode records = body == null ? null : objectMapper.readTree(body);
            if (records == null || !records.isArray() || records.isEmpty()) {
                throw new ExternalServiceException("Country directory returned no records");
            }
            return records;
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Country directory could not be loaded", e);
        }
    }
}
