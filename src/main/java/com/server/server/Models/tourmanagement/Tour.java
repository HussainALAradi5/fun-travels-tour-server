package com.server.server.models.tourmanagement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.server.server.enums.GenericStatus;
import com.server.server.models.City;
import com.server.server.models.Country;
import com.server.server.models.User;
import com.server.server.models.agency.Agency;
import com.server.server.models.agency.AgencyBranch;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@DynamicUpdate
@Table(name = "tours")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

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

    @NotNull(message = "Price is required")
    private Double price;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    @Builder.Default
    private GenericStatus status = GenericStatus.PENDING;

    @JsonProperty("hasTransportation")
    @Column(name = "has_transportation", nullable = false)
    @Builder.Default
    private boolean hasTransportation = false;

    // --- RELATIONSHIPS WITH CIRCULAR REFERENCE GUARDS ---

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

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", updatable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password", "tours", "authorities", "agency",
            "agencyBranch" })
    private User createdBy;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password", "tours", "authorities", "agency",
            "agencyBranch" })
    private User updatedBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

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
        if (this.price == null || this.price == 0.0)
            this.price = this.basePrice;
        this.totalPrice = this.basePrice - this.discountPrice;
        if (this.availableSlots == null && this.maxCapacity != null) {
            this.availableSlots = this.maxCapacity;
        }
    }

    @PostLoad
    private void applyDatabaseDefaults() {
        if (this.basePrice == null)
            this.basePrice = 0.0;
        if (this.price == null)
            this.price = this.basePrice;
        if (this.discountPrice == null)
            this.discountPrice = 0.0;
        if (this.availableSlots == null && this.maxCapacity != null) {
            this.availableSlots = this.maxCapacity;
        }
    }
}
