package com.server.server.constants;

import java.math.BigDecimal;

/** Numeric booking, pricing, refund, and scheduling rules. */
public final class BookingRules {

    public static final int HOLD_MINUTES = 15;
    /** Strictly more than this many days before departure earns a full refund. */
    public static final int FULL_REFUND_AFTER_DAYS = 14;
    /** At least this many days before departure earns a half refund. */
    public static final int HALF_REFUND_MIN_DAYS = 7;
    public static final BigDecimal HALF_REFUND_RATE = new BigDecimal("0.50");
    public static final BigDecimal KIDS_CHAIR_DISCOUNT_RATE = new BigDecimal("0.50");
    public static final int TOUR_CANCELLATION_MIN_DAYS = 14;
    public static final double TOUR_CANCELLATION_MAX_BOOKED_RATIO = 1.0 / 3.0;
    public static final int REMINDER_DAYS_BEFORE_TOUR = 7;
    public static final int PASSWORD_RESET_MINUTES = 15;
    public static final int MIN_PASSWORD_LENGTH = 6;

    public static final int QR_SIZE = 250;
    public static final int BARCODE_WIDTH = 300;
    public static final int BARCODE_HEIGHT = 100;

    public static final String CURRENCY = "USD";

    private BookingRules() {
    }
}
