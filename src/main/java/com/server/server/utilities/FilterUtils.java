package com.server.server.utilities;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

public final class FilterUtils {

    private FilterUtils() {}

    public static <T extends Comparable<? super T>> void validateRange(
            T start, T end, String startField, String endField) {
        if (start != null && end != null && start.compareTo(end) > 0) {
            throw new IllegalArgumentException(startField + " must be before or equal to " + endField + ".");
        }
    }

    public static <T> Specification<T> localDateRange(String field, LocalDate start, LocalDate end) {
        validateRange(start, end, "startDate", "endDate");
        return (root, query, cb) -> {
            if (start != null && end != null) {
                return cb.between(root.<LocalDate>get(field), start, end);
            }
            if (start != null) return cb.greaterThanOrEqualTo(root.<LocalDate>get(field), start);
            if (end != null) return cb.lessThanOrEqualTo(root.<LocalDate>get(field), end);
            return cb.conjunction();
        };
    }

    /** Uses an exclusive next-day upper bound so endDate includes the complete day. */
    public static <T> Specification<T> localDateTimeRange(String field, LocalDate start, LocalDate end) {
        validateRange(start, end, "startDate", "endDate");
        LocalDateTime from = start == null ? null : start.atStartOfDay();
        LocalDateTime toExclusive = end == null ? null : end.plusDays(1).atStartOfDay();
        return (root, query, cb) -> {
            if (from != null && toExclusive != null) {
                return cb.and(
                        cb.greaterThanOrEqualTo(root.<LocalDateTime>get(field), from),
                        cb.lessThan(root.<LocalDateTime>get(field), toExclusive));
            }
            if (from != null) return cb.greaterThanOrEqualTo(root.<LocalDateTime>get(field), from);
            if (toExclusive != null) return cb.lessThan(root.<LocalDateTime>get(field), toExclusive);
            return cb.conjunction();
        };
    }
}
