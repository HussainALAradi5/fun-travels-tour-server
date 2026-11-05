package com.server.server.utilities.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.utilities.pricing.PricingCalculator.TicketPrice;

class PricingCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    @Test
    void standardSeatAddsModifierAndMeals() {
        TicketPrice price = PricingCalculator.ticketPrice(new BigDecimal("100"), ChairType.STANDARD,
                new BigDecimal("15"), List.of(new BigDecimal("10"), new BigDecimal("5.50")));
        assertThat(price.total()).isEqualByComparingTo("130.50");
        assertThat(price.discount()).isEqualByComparingTo("0");
    }

    @Test
    void kidsChairHalvesTheTourPriceOnly() {
        TicketPrice price = PricingCalculator.ticketPrice(new BigDecimal("100"), ChairType.KIDS_CHAIR,
                new BigDecimal("10"), List.of(new BigDecimal("20")));
        assertThat(price.discount()).isEqualByComparingTo("50");
        assertThat(price.total()).isEqualByComparingTo("80");
    }

    @Test
    void missingValuesPriceAsZeroAndNeverNegative() {
        TicketPrice price = PricingCalculator.ticketPrice(null, null, new BigDecimal("-500"), null);
        assertThat(price.total()).isEqualByComparingTo("0");
    }

    @Test
    void fullRefundMoreThanTwoWeeksOut() {
        assertThat(PricingCalculator.refund(new BigDecimal("200"), TODAY.plusDays(15), TODAY).amount())
                .isEqualByComparingTo("200");
    }

    @Test
    void halfRefundBetweenOneAndTwoWeeks() {
        assertThat(PricingCalculator.refund(new BigDecimal("200"), TODAY.plusDays(14), TODAY).amount())
                .isEqualByComparingTo("100");
        assertThat(PricingCalculator.refund(new BigDecimal("200"), TODAY.plusDays(7), TODAY).amount())
                .isEqualByComparingTo("100");
    }

    @Test
    void noRefundInsideOneWeek() {
        assertThat(PricingCalculator.refund(new BigDecimal("200"), TODAY.plusDays(6), TODAY).amount())
                .isEqualByComparingTo("0");
    }

    @Test
    void nothingPaidMeansNothingRefunded() {
        assertThat(PricingCalculator.refund(null, TODAY.plusDays(30), TODAY).amount()).isEqualByComparingTo("0");
    }

    // An agency's own rules: kids 30% off; full refund more than 30 days out; 25% from 10 days.
    private static final PricingPolicy CUSTOM = new PricingPolicy(30, 30, 10, 25);

    @Test
    void kidsDiscountFollowsTheAgencyPolicy() {
        TicketPrice price = PricingCalculator.ticketPrice(new BigDecimal("200"), ChairType.KIDS_CHAIR, BigDecimal.ZERO,
                List.of(), CUSTOM);
        assertThat(price.discount()).isEqualByComparingTo("60.00");
        assertThat(price.total()).isEqualByComparingTo("140.00");
    }

    @Test
    void refundTiersFollowTheAgencyPolicy() {
        BigDecimal paid = new BigDecimal("100");
        assertThat(PricingCalculator.refund(paid, TODAY.plusDays(31), TODAY, CUSTOM).amount()).isEqualByComparingTo("100");
        assertThat(PricingCalculator.refund(paid, TODAY.plusDays(20), TODAY, CUSTOM).amount()).isEqualByComparingTo("25");
        assertThat(PricingCalculator.refund(paid, TODAY.plusDays(10), TODAY, CUSTOM).amount()).isEqualByComparingTo("25");
        assertThat(PricingCalculator.refund(paid, TODAY.plusDays(9), TODAY, CUSTOM).amount()).isEqualByComparingTo("0");
        assertThat(PricingCalculator.refund(paid, TODAY.plusDays(20), TODAY, CUSTOM).reason()).contains("25%");
    }

    @Test
    void noPartialRefundWhenItsPercentIsZero() {
        PricingPolicy strict = new PricingPolicy(0, 7, 3, 0);
        assertThat(PricingCalculator.refund(new BigDecimal("80"), TODAY.plusDays(5), TODAY, strict).amount()).isEqualByComparingTo("0");
        assertThat(PricingCalculator.refund(new BigDecimal("80"), TODAY.plusDays(8), TODAY, strict).amount()).isEqualByComparingTo("80");
        assertThat(PricingCalculator.ticketPrice(new BigDecimal("80"), ChairType.KIDS_CHAIR, BigDecimal.ZERO, List.of(), strict)
                .total()).isEqualByComparingTo("80");
    }
}
