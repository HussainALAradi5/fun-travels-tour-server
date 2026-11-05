package com.server.server.dto.agency;

import com.server.server.models.agency.Agency;
import com.server.server.utilities.pricing.PricingPolicy;

/**
 * An agency's discount and cancellation rules as customers see them before booking.
 * @param summary the rules in one sentence, e.g. "Kids chairs 50% off. Full refund more than 14 days before ..."
 */
public record AgencyPolicyResponse(
        Integer agencyId,
        int kidsDiscountPercent,
        int fullRefundDays,
        int partialRefundDays,
        int partialRefundPercent,
        String summary) {

    public static AgencyPolicyResponse from(Agency agency) {
        PricingPolicy policy = PricingPolicy.of(agency);
        return new AgencyPolicyResponse(agency == null ? null : agency.getId(), policy.kidsDiscountPercent(),
                policy.fullRefundDays(), policy.partialRefundDays(), policy.partialRefundPercent(), summary(policy));
    }

    static String summary(PricingPolicy p) {
        String kids = p.kidsDiscountPercent() > 0 ? "Kids chairs " + p.kidsDiscountPercent() + "% off. "
                : "No kids discount. ";
        String partial = p.partialRefundPercent() > 0 && p.partialRefundDays() <= p.fullRefundDays()
                ? " " + p.partialRefundPercent() + "% refund from " + p.partialRefundDays() + " to " + p.fullRefundDays()
                        + " days before;"
                : "";
        int noneBefore = p.partialRefundPercent() > 0 ? p.partialRefundDays() : p.fullRefundDays() + 1;
        return kids + "Full refund more than " + p.fullRefundDays() + " days before the tour;" + partial
                + " no refund within " + noneBefore + " days. If the agency cancels, you get a full refund.";
    }
}
