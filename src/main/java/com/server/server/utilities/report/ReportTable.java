package com.server.server.utilities.report;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything a renderer needs: title, author, time, headings, and rows of plain values (numbers, dates,
 * booleans, enums, text). {@code truncated} is set when rows were cut at the report row limit.
 */
public record ReportTable(String title, String generatedBy, LocalDateTime generatedAt, List<String> headers,
        List<List<Object>> rows, boolean truncated) {

    /** Reads the columns of each record into plain values. */
    public static <T> ReportTable of(String title, String generatedBy, List<ReportColumn<T>> columns, List<T> records,
            boolean truncated) {
        List<String> headers = columns.stream().map(ReportColumn::header).toList();
        List<List<Object>> rows = records.stream()
                .map(record -> columns.stream().map(column -> column.value().apply(record)).toList())
                .toList();
        return new ReportTable(title, generatedBy, LocalDateTime.now(), headers, rows, truncated);
    }
}
