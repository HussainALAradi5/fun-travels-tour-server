package com.server.server.utilities;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.server.server.enums.common.GenericStatus;
import com.server.server.exceptions.WorkflowException;

class DomainWorkflowValidatorTest {

    @ParameterizedTest
    @CsvSource({ "PENDING,APPROVED", "APPROVED,ACTIVE", "ACTIVE,COMPLETED", "ACTIVE,CANCELLED", "CANCELLED,PENDING" })
    void allowsLegalTourTransitions(GenericStatus from, GenericStatus to) {
        assertThatCode(() -> DomainWorkflowValidator.validateTour(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({ "PENDING,ACTIVE", "COMPLETED,CANCELLED", "CANCELLED,ACTIVE", "APPROVED,COMPLETED" })
    void rejectsIllegalTourTransitions(GenericStatus from, GenericStatus to) {
        assertThatThrownBy(() -> DomainWorkflowValidator.validateTour(from, to)).isInstanceOf(WorkflowException.class);
    }

    @Test
    void cancelledReservationCannotBeCancelledAgain() {
        assertThatThrownBy(() -> DomainWorkflowValidator.validateReservation(GenericStatus.REJECTED, GenericStatus.CANCELLED))
                .isInstanceOf(WorkflowException.class);
        assertThatThrownBy(() -> DomainWorkflowValidator.validateReservation(GenericStatus.COMPLETED, GenericStatus.CANCELLED))
                .isInstanceOf(WorkflowException.class);
    }

    @Test
    void staffCannotConfirmWithoutPayment() {
        assertThatThrownBy(() -> DomainWorkflowValidator.validateReservationAdministrativeTransition(
                GenericStatus.PENDING, GenericStatus.CONFIRMED)).isInstanceOf(WorkflowException.class);
    }

    @Test
    void staffCanRejectPendingAndCompleteConfirmed() {
        assertThatCode(() -> DomainWorkflowValidator.validateReservationAdministrativeTransition(
                GenericStatus.PENDING, GenericStatus.REJECTED)).doesNotThrowAnyException();
        assertThatCode(() -> DomainWorkflowValidator.validateReservationAdministrativeTransition(
                GenericStatus.CONFIRMED, GenericStatus.COMPLETED)).doesNotThrowAnyException();
    }
}
