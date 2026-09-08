package com.server.server.utilities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.models.tourmanagement.MealPlan;
import com.server.server.models.tourmanagement.Seat;

public class PricingCalculator {

    public static BigDecimal calculateTotal(BigDecimal basePrice, Seat seat, BigDecimal seatModifier,
            List<MealPlan> selectedMeals) {
        if (basePrice == null) basePrice = BigDecimal.ZERO;

        BigDecimal discount = BigDecimal.ZERO;

        if (seat != null && seat.getChairType() != null) {
            switch (seat.getChairType()) {
                case KIDS_CHAIR:
                    discount = basePrice.multiply(new BigDecimal("0.50"));
                    break;
                case WHEELCHAIR_ACCESSIBLE:
                    discount = basePrice.multiply(new BigDecimal("0.10"));
                    break;
                default:
                    break;
            }
        }

        BigDecimal seatMod = (seatModifier != null) ? seatModifier : BigDecimal.ZERO;

        BigDecimal mealsTotal = BigDecimal.ZERO;
        if (selectedMeals != null) {
            for (MealPlan mp : selectedMeals) {
                if (mp.getMealPrice() != null) {
                    mealsTotal = mealsTotal.add(BigDecimal.valueOf(mp.getMealPrice()));
                }
            }
        }

        return basePrice.subtract(discount).add(seatMod).add(mealsTotal);
    }

    public static BigDecimal calculateRefund(BigDecimal totalPaid, LocalDate tourStartDate) {
        if (totalPaid == null || tourStartDate == null) return BigDecimal.ZERO;

        long daysUntilTour = ChronoUnit.DAYS.between(LocalDate.now(), tourStartDate);

        if (daysUntilTour > 14) {
            return totalPaid;
        } else if (daysUntilTour >= 7) {
            return totalPaid.multiply(new BigDecimal("0.50"));
        } else {
            return BigDecimal.ZERO;
        }
    }
}
