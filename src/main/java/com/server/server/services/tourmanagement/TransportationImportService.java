package com.server.server.services.tourmanagement;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.server.server.enums.tourmanagement.TransportationType;
import com.server.server.dto.common.ImportResult;
import com.server.server.dto.tourmanagement.TransportationCreateRequest;
import com.server.server.services.common.ExcelImportService;
import lombok.RequiredArgsConstructor;

/** Bulk vehicle registration from a spreadsheet; each row goes through the normal create rules. */
@Service
@RequiredArgsConstructor
public class TransportationImportService {

    private final TransportationService transportationService;
    private final ExcelImportService excelImportService;

    public ImportResult importExcel(InputStream input) throws IOException {
        List<Map<String, String>> rows = excelImportService.readRowsByHeader(input);
        List<String> errors = new ArrayList<>();
        int imported = 0;
        for (Map<String, String> row : rows) {
            try {
                transportationService.create(toRequest(row));
                imported++;
            } catch (RuntimeException exception) {
                errors.add("Row " + row.get(ExcelImportService.ROW_NUMBER_KEY) + ": " + exception.getMessage());
            }
        }
        return new ImportResult(imported, errors.size(), errors);
    }

    private TransportationCreateRequest toRequest(Map<String, String> row) {
        Map<String, Integer> seats = new LinkedHashMap<>();
        putSeatCount(seats, "PREMIUM_RECLINER", row.get("premiumseats"));
        putSeatCount(seats, "WHEELCHAIR_ACCESSIBLE", row.get("accessibleseats"));
        putSeatCount(seats, "KIDS_CHAIR", row.get("kidsseats"));
        return new TransportationCreateRequest(
                required(row, "transportationnumber"),
                required(row, "code"),
                parseEnum(TransportationType.class, required(row, "type"), "type"),
                required(row, "providername"),
                positiveInteger(row, "totalcapacity"),
                positiveInteger(row, "agencyid"),
                optionalInteger(row, "branchid"),
                seats);
    }

    private String required(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required.");
        return value;
    }

    private Integer positiveInteger(Map<String, String> row, String key) {
        Integer value = optionalInteger(row, key);
        if (value == null || value < 1) throw new IllegalArgumentException(key + " must be a positive integer.");
        return value;
    }

    private Integer optionalInteger(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) return null;
        try {
            return new java.math.BigDecimal(value).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be a whole number.");
        }
    }

    private void putSeatCount(Map<String, Integer> seats, String type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return;
        try {
            int count = new java.math.BigDecimal(rawValue).intValueExact();
            if (count < 0) throw new IllegalArgumentException(type + " seat count cannot be negative.");
            seats.put(type, count);
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(type + " seat count must be a whole number.");
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String field) {
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        }
    }
}
