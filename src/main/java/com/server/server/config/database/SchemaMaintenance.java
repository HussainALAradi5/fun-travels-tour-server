package com.server.server.config.database;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ordered, idempotent schema corrections that {@code ddl-auto=update} cannot apply because it
 * never drops constraints. Each step checks the catalog first, so it is safe on every start.
 *
 * <p>1. Tickets used to map their seat one-to-one, which created a unique key on
 * {@code tickets.seat_id}: a seat could appear on only one ticket ever, even after cancellation
 * or on another tour. Seats are now many-to-one, so that unique key is dropped.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class SchemaMaintenance implements ApplicationRunner {

    private static final String UNIQUE_SEAT_CONSTRAINTS = """
            SELECT tc.constraint_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.constraint_column_usage ccu
              ON tc.constraint_name = ccu.constraint_name AND tc.table_schema = ccu.table_schema
            WHERE tc.table_name = 'tickets' AND tc.constraint_type = 'UNIQUE' AND ccu.column_name = 'seat_id'
            """;

    /** Columns whose fields were removed; left in place they would be NOT NULL or stale. */
    private static final List<String> RETIRED_COLUMNS = List.of(
            "ALTER TABLE IF EXISTS tours DROP COLUMN IF EXISTS price",
            "ALTER TABLE IF EXISTS transportations DROP COLUMN IF EXISTS remaining_seats");

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        RETIRED_COLUMNS.forEach(jdbcTemplate::execute);
        List<String> constraints = jdbcTemplate.queryForList(UNIQUE_SEAT_CONSTRAINTS, String.class);
        for (String constraint : constraints) {
            if (!constraint.matches("[A-Za-z0-9_]+")) {
                continue;
            }
            jdbcTemplate.execute("ALTER TABLE tickets DROP CONSTRAINT " + constraint);
            log.info("Dropped legacy unique constraint {} on tickets.seat_id", constraint);
        }
    }
}
