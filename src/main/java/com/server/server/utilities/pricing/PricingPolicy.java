package com.server.server.utilities.pricing;

import java.math.BigDecimal;

import com.server.server.constants.tourmanagement.BookingRules;
import com.server.server.models.agency.Agency;
import com.server.server.models.agency.AgencyPolicy;

/** The discount and refund rules a price or refund is calculated with: the tour's agency policy, or the defaults. */
public record PricingPolicy(int kidsDiscountPercent, int fullRefundDays, int partialRefundDays, int partialRefundPercent) {

    public static final PricingPolicy DEFAULT = new PricingPolicy(BookingRules.DEFAULT_KIDS_DISCOUNT_PERCENT,
            BookingRules.FULL_REFUND_AFTER_DAYS, BookingRules.HALF_REFUND_MIN_DAYS,
            BookingRules.DEFAULT_PARTIAL_REFUND_PERCENT);

    public static PricingPolicy of(Agency agency) {
        AgencyPolicy policy = agency == null ? null : agency.getPolicy();
        return policy == null ? DEFAULT : new PricingPolicy(policy.getKidsDiscountPercent(), policy.getFullRefundDays(),
                policy.getPartialRefundDays(), policy.getPartialRefundPercent());
    }

    BigDecimal kidsDiscountRate() {
        return BigDecimal.valueOf(kidsDiscountPercent).movePointLeft(2);
    }

    BigDecimal partialRefundRate() {
        return BigDecimal.valueOf(partialRefundPercent).movePointLeft(2);
    }
}
