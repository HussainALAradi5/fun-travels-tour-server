package com.server.server.utilities.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** How report values are shown as text, shared by the PDF and Excel renderers. */
public final class ReportCells {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private ReportCells() {
    }

    public static String text(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof Boolean flag) {
            return flag ? "Yes" : "No";
        }
        if (isDecimal(value)) {
            // Prices and amounts are decimals; they always show two places ("100.00").
            return String.format(Locale.US, "%,.2f", ((Number) value).doubleValue());
        }
        if (value instanceof LocalDateTime time) {
            return time.format(DATE_TIME);
        }
        if (value instanceof Enum<?> constant) {
            return constant.name().replace('_', ' ');
        }
        String text = value.toString().trim();
        return text.isEmpty() ? "-" : text;
    }

    /** Money and other fractional values, as opposed to counts. */
    public static boolean isDecimal(Object value) {
        return value instanceof BigDecimal || value instanceof Double || value instanceof Float;
    }

    public static boolean isNumeric(Object value) {
        return value instanceof Number;
    }

    public static boolean isDate(Object value) {
        return value instanceof LocalDate || value instanceof LocalDateTime;
    }

    /** "Tour Inventory" at 15:30 -> "Tour_Inventory_2026-11-04_1530". */
    public static String fileStem(String title, LocalDateTime at) {
        return title.replaceAll("[^A-Za-z0-9]+", "_") + "_" + at.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
    }
}
