package com.server.server.models.user;

import com.server.server.constants.ValidationPatterns;
import com.server.server.models.base.BaseEntity;

import com.server.server.models.payment.Account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.server.server.enums.user.UserTypeEnum;
import com.server.server.models.agency.Agency;
import com.server.server.models.agency.AgencyBranch;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User extends BaseEntity {

    @NotBlank(message = "Username is required")
    private String userName;

    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private int age;

    @Enumerated(EnumType.STRING)
    private UserTypeEnum userType;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = ValidationPatterns.MOBILE_NUMBER, message = ValidationPatterns.MOBILE_NUMBER_MESSAGE)
    @Column(name = "mobile_number", nullable = false, unique = true)
    private String mobileNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agency_id")
    @JsonIgnoreProperties({ "employees", "branches", "agencyOwner" })
    private Agency agency;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agency_branch_id")
    @JsonIgnoreProperties({ "employees", "branchManager", "agency" })
    private AgencyBranch agencyBranch;

    @Column(name = "is_active")
    @JsonProperty("active")
    private boolean isActive = true;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Getter(onMethod_ = { @JsonProperty("base64Image") })
    @Setter(onMethod_ = { @JsonProperty("base64Image") })
    private String base64Image;

    @Column(name = "reset_token")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private java.time.LocalDateTime resetTokenExpiry;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("user")
    private Account account;

}
