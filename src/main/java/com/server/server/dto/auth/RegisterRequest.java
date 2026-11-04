package com.server.server.dto.auth;

import com.server.server.constants.ValidationPatterns;
import com.server.server.utilities.MobileNumbers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record RegisterRequest(
        @NotBlank(message = "Username is required") @Size(max = 50, message = "Username must be at most 50 characters") String userName,
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password,
        @NotBlank(message = "Mobile number is required") @Pattern(regexp = ValidationPatterns.MOBILE_NUMBER, message = ValidationPatterns.MOBILE_NUMBER_MESSAGE) String mobileNumber,
        @Min(value = 0, message = "Age cannot be negative") @Max(value = 120, message = "Age must be realistic") Integer age) {

    /** Spaces, dashes, and a leading 00 are removed before validation, so "+973 3999 0001" is accepted. */
    public RegisterRequest {
        mobileNumber = MobileNumbers.clean(mobileNumber);
    }
}
