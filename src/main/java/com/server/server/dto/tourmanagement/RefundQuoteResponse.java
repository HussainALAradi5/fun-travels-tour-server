package com.server.server.dto.tourmanagement;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.server.server.utilities.pricing.PricingCalculator;
import com.server.server.utilities.pricing.PricingCalculator.RefundQuote;

/**
 * What cancelling now would give back, calculated with the same rules as the cancellation itself.
 * @param paid what is still active on the booking or ticket
 * @param refund the amount that would return to the wallet
 * @param percent refund as a share of what was paid, 0 to 100
 * @param daysUntilTour days left before the tour starts
 * @param reason the rule that applies, e.g. "50% refund (7 to 14 days notice)"
 */
public record RefundQuoteResponse(BigDecimal paid, BigDecimal refund, int percent, long daysUntilTour, String reason) {

    public static RefundQuoteResponse of(BigDecimal paid, RefundQuote quote, long daysUntilTour) {
        BigDecimal amount = PricingCalculator.money(paid);
        int percent = amount.signum() == 0 ? 0
                : quote.amount().multiply(BigDecimal.valueOf(100)).divide(amount, 0, RoundingMode.HALF_UP).intValue();
        return new RefundQuoteResponse(amount, quote.amount(), percent, daysUntilTour, quote.reason());
    }
}
