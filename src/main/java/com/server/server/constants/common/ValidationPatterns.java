package com.server.server.constants.common;

/** Shared validation patterns and their messages. */
public final class ValidationPatterns {

    /** E.164 phone number: "+", the country code, then the subscriber number (for example +97339990001). */
    public static final String MOBILE_NUMBER = "^\\+[1-9]\\d{7,14}$";
    public static final String MOBILE_NUMBER_MESSAGE = "Mobile number must start with + and the country code, for example +97339990001";

    private ValidationPatterns() {
    }
}
