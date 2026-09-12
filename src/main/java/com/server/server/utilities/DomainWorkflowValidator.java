package com.server.server.utilities;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.server.server.enums.GenericStatus;
import com.server.server.exceptions.WorkflowException;

public final class DomainWorkflowValidator {
    private static final Map<GenericStatus, Set<GenericStatus>> TOUR_TRANSITIONS = Map.of(
            GenericStatus.PENDING, EnumSet.of(GenericStatus.APPROVED, GenericStatus.CANCELLED),
            GenericStatus.APPROVED, EnumSet.of(GenericStatus.ACTIVE, GenericStatus.CANCELLED),
            GenericStatus.ACTIVE, EnumSet.of(GenericStatus.COMPLETED, GenericStatus.CANCELLED),
            GenericStatus.CANCELLED, EnumSet.of(GenericStatus.PENDING),
            GenericStatus.COMPLETED, EnumSet.noneOf(GenericStatus.class));

    private static final Map<GenericStatus, Set<GenericStatus>> RESERVATION_TRANSITIONS = Map.of(
            GenericStatus.PENDING, EnumSet.of(GenericStatus.CONFIRMED, GenericStatus.CANCELLED, GenericStatus.REJECTED),
            GenericStatus.APPROVED, EnumSet.of(GenericStatus.CONFIRMED, GenericStatus.CANCELLED),
            GenericStatus.CONFIRMED, EnumSet.of(GenericStatus.COMPLETED, GenericStatus.CANCELLED),
            GenericStatus.REJECTED, EnumSet.noneOf(GenericStatus.class),
            GenericStatus.CANCELLED, EnumSet.noneOf(GenericStatus.class),
            GenericStatus.COMPLETED, EnumSet.noneOf(GenericStatus.class));

    private DomainWorkflowValidator() {}

    public static void validateTour(GenericStatus current, GenericStatus target) {
        validate("tour", TOUR_TRANSITIONS, current, target);
    }

    public static void validateReservation(GenericStatus current, GenericStatus target) {
        validate("reservation", RESERVATION_TRANSITIONS, current, target);
    }

    private static void validate(String resource, Map<GenericStatus, Set<GenericStatus>> transitions,
            GenericStatus current, GenericStatus target) {
        if (current == target) return;
        if (!transitions.getOrDefault(current, Set.of()).contains(target)) {
            throw new WorkflowException("Invalid " + resource + " transition: " + current + " -> " + target);
        }
    }
}
