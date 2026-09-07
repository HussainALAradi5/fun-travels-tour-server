package com.server.server.models.agency;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.server.server.models.City;
import com.server.server.models.Country;
import com.server.server.models.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "agency_branch", uniqueConstraints = {
        @UniqueConstraint(name = "UniqueBranchNamePerAgency", columnNames = { "branchName", "agency_id" })
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class AgencyBranch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String branchName;
    private String branchAddress;
    private String contactNumber;
    private String ownerMobileNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agency_id")
    @JsonIgnoreProperties({ "branches", "employees", "agencyOwner" })
    private Agency agency;

    @Column(name = "is_active")
    @JsonProperty("active")
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Country country;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private City city;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manager_id")
    @JsonIgnoreProperties({ "agencyBranch", "agency" })
    private User branchManager;

    @OneToMany(mappedBy = "agencyBranch", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("agencyBranch")
    private List<User> employees;
}