package com.server.server.models.tourmanagement;

import com.server.server.models.base.BaseEntity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.server.server.enums.common.GenericStatus;
import com.server.server.models.geography.City;
import com.server.server.models.geography.Country;
import com.server.server.models.agency.Agency;
import com.server.server.models.agency.AgencyBranch;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@DynamicUpdate
@Table(name = "tours")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Tour extends BaseEntity {

    @Column(name = "tour_number", unique = true, nullable = false)
    @NotBlank(message = "Tour code is required")
    private String tourNumber;

    @Column(nullable = false)
    @NotBlank(message = "Title is required")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(name = "number_of_days")
    private Integer numberOfDays;

    @NotNull(message = "Max capacity is required")
    private Integer maxCapacity;

    @Column(name = "available_slots")
    private Integer availableSlots;


    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    @Builder.Default
    private GenericStatus status = GenericStatus.PENDING;

    @JsonProperty("hasTransportation")
    @Column(name = "has_transportation", nullable = false)
    @Builder.Default
    private boolean hasTransportation = false;

    // Relationships (JSON back-references are ignored to avoid cycles)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transportation_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "agency", "tours", "seats" })
    private Transportation transportation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_country_id")
    @NotNull(message = "Start Country is required")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "tours", "agencies", "cities" })
    private Country startCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_country_id")
    @NotNull(message = "End Country is required")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "tours", "agencies", "cities" })
    private Country endCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_city_id")
    @NotNull(message = "Start City is required")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "country", "tours" })
    private City startCity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_city_id")
    @NotNull(message = "End City is required")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "country", "tours" })
    private City endCity;

    @ManyToMany
    @JoinTable(name = "tour_destinations", joinColumns = @JoinColumn(name = "tour_id"), inverseJoinColumns = @JoinColumn(name = "country_id"))
    @Builder.Default
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "tours" })
    private Set<Country> destinationCountries = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "owner", "branches", "tours", "transportations" })
    private Agency agency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "manager", "agency", "tours" })
    private AgencyBranch agencyBranch;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("tour")
    private List<Ticket> tickets;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<TourReservation> reservations;



    @NotNull(message = "Base price is required")
    private Double basePrice;

    @Builder.Default
    private Double discountPrice = 0.0;

    private Double totalPrice;

    @ManyToMany
    @JoinTable(name = "tour_available_meals", joinColumns = @JoinColumn(name = "tour_id"), inverseJoinColumns = @JoinColumn(name = "meal_plan_id"))
    @Builder.Default
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "agency", "tours" })
    private Set<MealPlan> availableMeals = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void ensureDataIntegrity() {
        if (this.basePrice == null)
            this.basePrice = 0.0;
        if (this.discountPrice == null)
            this.discountPrice = 0.0;
        this.totalPrice = this.basePrice - this.discountPrice;
        if (this.availableSlots == null && this.maxCapacity != null) {
            this.availableSlots = this.maxCapacity;
        }
    }

    @PostLoad
    private void applyDatabaseDefaults() {
        if (this.basePrice == null)
            this.basePrice = 0.0;
        if (this.discountPrice == null)
            this.discountPrice = 0.0;
        if (this.availableSlots == null && this.maxCapacity != null) {
            this.availableSlots = this.maxCapacity;
        }
    }
}
