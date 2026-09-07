package com.server.server.models.tourmanagement;

import java.util.List;

import org.hibernate.annotations.Formula;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.TransportationStatus;
import com.server.server.enums.tourmanagement.TransportationType;
import com.server.server.models.agency.Agency;

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
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transportations")
public class Transportation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Transportation number is required")
    private String transportationNumber;

    @NotBlank(message = "Transportation code is required")
    private String code;

    @OneToMany(mappedBy = "transportation", fetch = FetchType.LAZY)
    @JsonIgnore // Prevents infinite recursion in JSON
    private List<Tour> tours;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Transportation Type is required")
    private TransportationType type;

    private String providerName;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    private GenericStatus status = GenericStatus.ACTIVE;

    @NotNull(message = "Transportation total capacity is required")
    private Integer totalCapacity;

    @Column(name = "remaining_seats")
    private Integer remainingSeats;

    // This annotation calculates the count directly from the DB for read operations
    @Formula("(SELECT COUNT(*) FROM seats s WHERE s.transportation_id = id AND s.status = 'AVAILABLE')")
    private Integer calculatedAvailable;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Transportation Unit Status is required")
    @Column(name = "unit_status")
    private TransportationStatus unitStatus = TransportationStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    @JsonBackReference(value = "agency-transportations")
    private Agency agency;

    @OneToMany(mappedBy = "transportation", cascade = CascadeType.ALL)
    @JsonManagedReference(value = "transportation-seats")
    private List<Seat> seats;

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private java.util.Map<String, Integer> seatConfig;

}