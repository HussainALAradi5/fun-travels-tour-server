package com.server.server.utilities.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import com.server.server.constants.tourmanagement.BookingRules;
import com.server.server.enums.tourmanagement.ChairType;

/** Pure pricing and refund rules shared by tickets, reservations, and tour cancellation. */
public final class PricingCalculator {

    private static final int MONEY_SCALE = 2;

    private PricingCalculator() {
    }

    public record TicketPrice(BigDecimal base, BigDecimal discount, BigDecimal seatModifier,
            BigDecimal meals, BigDecimal total) {
    }

    public record RefundQuote(BigDecimal amount, String reason) {
    }

    /** Ticket total = (tour price - kids discount) + seat modifier + selected meals. */
    public static TicketPrice ticketPrice(BigDecimal tourPrice, ChairType chairType, BigDecimal seatModifier,
            List<BigDecimal> mealPrices) {
        BigDecimal base = money(tourPrice);
        BigDecimal discount = chairType == ChairType.KIDS_CHAIR
                ? money(base.multiply(BookingRules.KIDS_CHAIR_DISCOUNT_RATE))
                : money(BigDecimal.ZERO);
        BigDecimal modifier = money(seatModifier);
        BigDecimal meals = money(mealPrices == null ? BigDecimal.ZERO
                : mealPrices.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal total = base.subtract(discount).add(modifier).add(meals);
        if (total.signum() < 0) {
            total = money(BigDecimal.ZERO);
        }
        return new TicketPrice(base, discount, modifier, meals, total);
    }

    /** Full refund more than two weeks out, half refund one to two weeks out, none inside a week. */
    public static RefundQuote refund(BigDecimal paid, LocalDate tourStart, LocalDate today) {
        BigDecimal amount = money(paid);
        if (tourStart == null || amount.signum() <= 0) {
            return new RefundQuote(money(BigDecimal.ZERO), "Nothing was paid.");
        }
        long daysUntilTour = ChronoUnit.DAYS.between(today, tourStart);
        if (daysUntilTour > BookingRules.FULL_REFUND_AFTER_DAYS) {
            return new RefundQuote(amount, "Full refund (more than two weeks notice)");
        }
        if (daysUntilTour >= BookingRules.HALF_REFUND_MIN_DAYS) {
            return new RefundQuote(money(amount.multiply(BookingRules.HALF_REFUND_RATE)),
                    "50% refund (one to two weeks notice)");
        }
        return new RefundQuote(money(BigDecimal.ZERO), "No refund (less than one week notice)");
    }

    public static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal money(Double value) {
        return money(value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value));
    }
}
