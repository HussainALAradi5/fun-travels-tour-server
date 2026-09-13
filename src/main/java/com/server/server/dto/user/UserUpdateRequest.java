package com.server.server.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {
    private String name;
    private String mobileNumber;
    private Integer age;
    private String profileImageUrl;
    private String base64Image;
}
