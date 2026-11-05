package com.server.server.models.agency;

import com.server.server.constants.tourmanagement.BookingRules;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An agency's own discount and cancellation rules; new agencies start with the platform defaults in
 * {@link BookingRules}. Refunds: the full price more than {@code fullRefundDays} before the tour, then
 * {@code partialRefundPercent} from {@code partialRefundDays} days before, and nothing after that.
 */
@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class AgencyPolicy {

    @Column(name = "kids_discount_percent", nullable = false)
    private int kidsDiscountPercent = BookingRules.DEFAULT_KIDS_DISCOUNT_PERCENT;

    @Column(name = "full_refund_days", nullable = false)
    private int fullRefundDays = BookingRules.FULL_REFUND_AFTER_DAYS;

    @Column(name = "partial_refund_days", nullable = false)
    private int partialRefundDays = BookingRules.HALF_REFUND_MIN_DAYS;

    @Column(name = "partial_refund_percent", nullable = false)
    private int partialRefundPercent = BookingRules.DEFAULT_PARTIAL_REFUND_PERCENT;
}
