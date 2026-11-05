package com.server.server.utilities.report;

import java.awt.Color;
import java.util.Map;
import java.util.Set;

/**
 * The color a report value is shown in: statuses and yes/no as green, amber, or red; gender as blue or pink.
 * Values that are not a status stay plain. Both renderers read the same tone, so PDF and Excel agree.
 */
public enum ReportTone {
    PLAIN(new Color(30, 41, 59), null),
    GOOD(new Color(21, 128, 61), new Color(220, 252, 231)),
    WARN(new Color(180, 83, 9), new Color(254, 243, 199)),
    BAD(new Color(185, 28, 28), new Color(254, 226, 226)),
    INFO(new Color(29, 78, 216), new Color(219, 234, 254)),
    MALE(new Color(29, 78, 216), new Color(219, 234, 254)),
    FEMALE(new Color(190, 24, 93), new Color(252, 231, 243));

    private static final Set<String> GOOD_VALUES = Set.of("ACTIVE", "APPROVED", "CONFIRMED", "COMPLETED", "AVAILABLE",
            "WALLET_TOP_UP", "REFUND");
    private static final Set<String> WARN_VALUES = Set.of("PENDING", "RESERVED", "PARTIAL", "PARTIAL_REFUND",
            "MANUAL_ADJUSTMENT", "REFUNDED");
    private static final Set<String> BAD_VALUES = Set.of("CANCELLED", "REJECTED", "INACTIVE", "FAILED", "MAINTENANCE",
            "FULL", "CANCELLATION_FEE", "WITHDRAWAL");
    private static final Map<String, ReportTone> GENDER = Map.of("MALE", MALE, "FEMALE", FEMALE);

    /** Text color. */
    public final Color ink;
    /** Soft cell background, or null for none. */
    public final Color tint;

    ReportTone(Color ink, Color tint) {
        this.ink = ink;
        this.tint = tint;
    }

    public static ReportTone of(Object value) {
        if (value instanceof Boolean flag) {
            return flag ? GOOD : BAD;
        }
        if (!(value instanceof Enum<?> constant)) {
            return PLAIN;
        }
        String name = constant.name();
        if (GENDER.containsKey(name)) {
            return GENDER.get(name);
        }
        if (GOOD_VALUES.contains(name)) {
            return GOOD;
        }
        if (WARN_VALUES.contains(name)) {
            return WARN;
        }
        return BAD_VALUES.contains(name) ? BAD : INFO;
    }

    public boolean highlighted() {
        return tint != null;
    }
}
