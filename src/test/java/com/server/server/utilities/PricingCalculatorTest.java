package com.server.server.utilities;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.utilities.PricingCalculator.TicketPrice;

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
}
