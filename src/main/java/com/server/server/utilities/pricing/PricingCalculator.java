package com.server.server.utilities.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

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

    /** Ticket total with the default policy. */
    public static TicketPrice ticketPrice(BigDecimal tourPrice, ChairType chairType, BigDecimal seatModifier,
            List<BigDecimal> mealPrices) {
        return ticketPrice(tourPrice, chairType, seatModifier, mealPrices, PricingPolicy.DEFAULT);
    }

    /** Ticket total = (tour price - the policy's kids discount) + seat modifier + selected meals. */
    public static TicketPrice ticketPrice(BigDecimal tourPrice, ChairType chairType, BigDecimal seatModifier,
            List<BigDecimal> mealPrices, PricingPolicy policy) {
        BigDecimal base = money(tourPrice);
        BigDecimal discount = chairType == ChairType.KIDS_CHAIR
                ? money(base.multiply(policy.kidsDiscountRate()))
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

    /** Refund with the default policy. */
    public static RefundQuote refund(BigDecimal paid, LocalDate tourStart, LocalDate today) {
        return refund(paid, tourStart, today, PricingPolicy.DEFAULT);
    }

    /**
     * Full refund more than {@code fullRefundDays} before the tour, the partial percentage from
     * {@code partialRefundDays} days before, and nothing closer to the start.
     */
    public static RefundQuote refund(BigDecimal paid, LocalDate tourStart, LocalDate today, PricingPolicy policy) {
        BigDecimal amount = money(paid);
        if (tourStart == null || amount.signum() <= 0) {
            return new RefundQuote(money(BigDecimal.ZERO), "Nothing was paid.");
        }
        long daysUntilTour = ChronoUnit.DAYS.between(today, tourStart);
        if (daysUntilTour > policy.fullRefundDays()) {
            return new RefundQuote(amount, "Full refund (more than " + policy.fullRefundDays() + " days notice)");
        }
        if (daysUntilTour >= policy.partialRefundDays() && policy.partialRefundPercent() > 0) {
            return new RefundQuote(money(amount.multiply(policy.partialRefundRate())),
                    policy.partialRefundPercent() + "% refund (" + policy.partialRefundDays() + " to "
                            + policy.fullRefundDays() + " days notice)");
        }
        return new RefundQuote(money(BigDecimal.ZERO),
                "No refund (less than " + Math.max(policy.partialRefundDays(), 1) + " days notice)");
    }

    public static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal money(Double value) {
        return money(value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value));
    }
}
