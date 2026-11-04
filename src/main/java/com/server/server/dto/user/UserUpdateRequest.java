package com.server.server.dto.user;

import com.server.server.constants.ValidationPatterns;
import com.server.server.utilities.MobileNumbers;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * Self-service profile fields. Role, agency, email, and password are changed elsewhere.
 * @param base64Image Optional JPEG or PNG image as base64 or a data URL.
 * @param removeImage When true the current profile image is removed.
 */
public record UserUpdateRequest(
        String name,
        @Pattern(regexp = ValidationPatterns.MOBILE_NUMBER, message = ValidationPatterns.MOBILE_NUMBER_MESSAGE) String mobileNumber,
        @Min(value = 0, message = "Age cannot be negative") @Max(value = 120, message = "Age must be realistic") Integer age,
        String base64Image,
        boolean removeImage) {

    /** Spaces, dashes, and a leading 00 are removed before validation, so "+973 3999 0001" is accepted. */
    public UserUpdateRequest {
        mobileNumber = MobileNumbers.clean(mobileNumber);
    }
}
