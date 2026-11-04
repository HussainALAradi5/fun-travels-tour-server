package com.server.server.constants;

/** Shared validation patterns and their messages. */
public final class ValidationPatterns {

    /** E.164-style international phone number, with an optional leading plus. */
    public static final String MOBILE_NUMBER = "^\\+?[1-9]\\d{7,14}$";
    public static final String MOBILE_NUMBER_MESSAGE = "Mobile number must be a valid international phone number";

    private ValidationPatterns() {
    }
}
