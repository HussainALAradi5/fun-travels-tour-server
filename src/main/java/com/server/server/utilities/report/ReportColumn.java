package com.server.server.utilities.report;

import java.util.function.Function;

/** One report column: its heading and how to read the value from a record. */
public record ReportColumn<T>(String header, Function<T, Object> value) {

    public static <T> ReportColumn<T> of(String header, Function<T, Object> value) {
        return new ReportColumn<>(header, value);
    }
}
