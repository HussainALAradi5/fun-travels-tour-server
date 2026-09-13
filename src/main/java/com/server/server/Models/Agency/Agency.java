package com.server.server.models.agency;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.server.server.enums.UserTypeEnum;
import com.server.server.models.City;
import com.server.server.models.Country;
import com.server.server.models.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "agencies", uniqueConstraints = {
        @UniqueConstraint(name = "UniqueAgencyNamePerCountry", columnNames = { "agencyName", "country_id" })
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    @NotBlank(message = "Agency name is required")
    private String agencyName;

    private String address;
    private String contactNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id")
    @JsonIgnoreProperties({ "agency", "agencyBranch" })
    private User agencyOwner;

    @Enumerated(EnumType.STRING)
    private UserTypeEnum userType = UserTypeEnum.OWNER;

    private String ownerMobileNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Country country;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private City city;

    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("agency")
    private List<AgencyBranch> branches;

    @Column(name = "is_active")
    @JsonProperty("active")
    private boolean isActive = true;

    @OneToMany(mappedBy = "agency", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("agency")
    private List<User> employees;
}
