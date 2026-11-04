package com.server.server.dto.user;

import com.server.server.constants.ValidationPatterns;
import com.server.server.enums.user.UserTypeEnum;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Staff account created by an administrator, agency owner, or branch manager. */
@Data
public class EmployeeCreateRequest {
    @NotBlank(message = "Username is required")
    private String userName;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = ValidationPatterns.MOBILE_NUMBER, message = ValidationPatterns.MOBILE_NUMBER_MESSAGE)
    private String mobileNumber;

    @Min(value = 0, message = "Age cannot be negative")
    @Max(value = 120, message = "Age must be realistic")
    private Integer age;

    @NotNull(message = "Role is required")
    private UserTypeEnum userType;
}
