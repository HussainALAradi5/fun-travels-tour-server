package com.server.server.constants.errors;

/**
 * Business-rule error codes carried by {@code WorkflowException} (HTTP 409) in the {@code code} field;
 * clients branch on these. Generic codes such as {@code VALIDATION_FAILED} are in {@link ErrorCodes}.
 */
public final class WorkflowErrorCodes {
    public static final String ALREADY_CANCELLED = "ALREADY_CANCELLED";
    public static final String CAPACITY_BELOW_BOOKINGS = "CAPACITY_BELOW_BOOKINGS";
    public static final String CARD_DECLINED = "CARD_DECLINED";
    public static final String CUSTOMER_REQUIRED = "CUSTOMER_REQUIRED";
    public static final String DATE_CONFLICT = "DATE_CONFLICT";
    public static final String DISCOUNT_ABOVE_PRICE = "DISCOUNT_ABOVE_PRICE";
    public static final String DUPLICATE_MEAL = "DUPLICATE_MEAL";
    public static final String DUPLICATE_SEAT = "DUPLICATE_SEAT";
    public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
    public static final String INSUFFICIENT_CAPACITY = "INSUFFICIENT_CAPACITY";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String INVALID_RESET_LINK = "INVALID_RESET_LINK";
    public static final String INVALID_SEAT_FOR_TOUR = "INVALID_SEAT_FOR_TOUR";
    public static final String MEAL_INACTIVE = "MEAL_INACTIVE";
    public static final String MEAL_NOT_OFFERED = "MEAL_NOT_OFFERED";
    public static final String MEAL_OTHER_AGENCY = "MEAL_OTHER_AGENCY";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_METHOD_UNSUPPORTED = "PAYMENT_METHOD_UNSUPPORTED";
    public static final String PAYMENT_NOT_CAPTURED = "PAYMENT_NOT_CAPTURED";
    public static final String REPORT_PARAMETER_REQUIRED = "REPORT_PARAMETER_REQUIRED";
    public static final String REQUEST_CLOSED = "REQUEST_CLOSED";
    public static final String RESERVATION_HOLD_EXPIRED = "RESERVATION_HOLD_EXPIRED";
    public static final String RESERVATION_NOT_FOUND = "RESERVATION_NOT_FOUND";
    public static final String RESERVATION_NOT_PAYABLE = "RESERVATION_NOT_PAYABLE";
    public static final String SEAT_CHOICE_REQUIRED = "SEAT_CHOICE_REQUIRED";
    public static final String SEAT_IN_USE = "SEAT_IN_USE";
    public static final String SEAT_LAYOUT_LOCKED = "SEAT_LAYOUT_LOCKED";
    public static final String SEAT_NOT_AVAILABLE = "SEAT_NOT_AVAILABLE";
    public static final String SEAT_NOT_FOUND = "SEAT_NOT_FOUND";
    public static final String SEAT_STATUS_LOCKED = "SEAT_STATUS_LOCKED";
    public static final String TICKET_CODE_FAILED = "TICKET_CODE_FAILED";
    public static final String TICKET_NOT_PAID = "TICKET_NOT_PAID";
    public static final String TOUR_NOT_BOOKABLE = "TOUR_NOT_BOOKABLE";
    public static final String TRANSPORTATION_LOCKED = "TRANSPORTATION_LOCKED";

    private WorkflowErrorCodes() {
    }
}
