package com.server.server.utilities.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** How report values are shown as text, shared by the PDF and Excel renderers. */
public final class ReportCells {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);
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
        if (value instanceof LocalDate date) {
            return date.format(DATE);
        }
        if (value instanceof Enum<?> constant) {
            return words(constant.name());
        }
        String text = value.toString().trim();
        return text.isEmpty() ? "-" : text;
    }

    /** "SUPPORT_AGENT" -> "Support Agent". */
    public static String words(String constant) {
        StringBuilder out = new StringBuilder();
        for (String part : constant.toLowerCase(Locale.ENGLISH).split("_")) {
            if (!part.isEmpty()) {
                out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return out.toString();
    }

    /** Headings of money columns worth adding up; unit prices (a tour's or a meal's "Price") are not. */
    private static final Set<String> SUMMED = Set.of("Total", "Amount");

    /**
     * Column totals for a closing "Total" row: the sum of each "Total" or "Amount" money column, null elsewhere;
     * null when there is nothing to add up.
     */
    public static List<BigDecimal> totals(ReportTable table) {
        int columns = table.headers().size();
        List<BigDecimal> sums = new ArrayList<>(Collections.nCopies(columns, (BigDecimal) null));
        boolean any = false;
        for (int c = 0; c < columns; c++) {
            final int column = c;
            if (!SUMMED.contains(table.headers().get(c)) || table.rows().isEmpty() || !table.rows().stream().allMatch(row -> row.get(column) == null || isDecimal(row.get(column)))
                    || table.rows().stream().allMatch(row -> row.get(column) == null)) {
                continue;
            }
            sums.set(c, table.rows().stream().map(row -> row.get(column)).filter(v -> v != null)
                    .map(v -> new BigDecimal(v.toString())).reduce(BigDecimal.ZERO, BigDecimal::add));
            any = true;
        }
        return any ? sums : null;
    }

    /** Caption of a column's total: "Total" stays "Total", "Amount" becomes "Total amount". */
    public static String totalLabel(String header) {
        return "Total".equals(header) ? header : "Total " + header.toLowerCase(Locale.ENGLISH);
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
