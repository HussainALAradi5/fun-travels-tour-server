package com.server.server.seeds.reference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.server.server.services.geography.CountryDirectoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reference data, not demo data: on startup, when the database has fewer than {@code app.countries.minimum}
 * countries, every country is imported from the public directory. A failed import is logged and retried on the
 * next start; it never stops the server.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class CountryDirectoryImporter implements ApplicationRunner {

    private final CountryDirectoryService directory;

    @Value("${app.countries.import-on-startup:true}")
    private boolean enabled;

    @Value("${app.countries.minimum:200}")
    private int minimum;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || directory.countryCount() >= minimum) {
            return;
        }
        try {
            directory.importAll();
        } catch (Exception e) {
            log.warn("Country directory import skipped: {}", e.getMessage());
        }
    }
}
