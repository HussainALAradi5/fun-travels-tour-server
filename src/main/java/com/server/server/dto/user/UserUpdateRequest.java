package com.server.server.dto.user;

import com.server.server.constants.ValidationPatterns;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Self-service profile fields. Role, agency, email, and password are changed elsewhere. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {
    private String name;

    @Pattern(regexp = ValidationPatterns.MOBILE_NUMBER, message = ValidationPatterns.MOBILE_NUMBER_MESSAGE)
    private String mobileNumber;

    @Min(value = 0, message = "Age cannot be negative")
    @Max(value = 120, message = "Age must be realistic")
    private Integer age;

    /** Optional JPEG or PNG image as base64 or a data URL. */
    private String base64Image;

    /** When true the current profile image is removed. */
    private boolean removeImage;
}
