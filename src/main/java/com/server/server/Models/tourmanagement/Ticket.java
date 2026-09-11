package com.server.server.models.tourmanagement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.models.User;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tickets")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    @JsonIgnoreProperties({ "reservations", "agency", "transportation" })
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({ "password", "agency", "agencyBranch" })
    private User customer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    @JsonIgnoreProperties({ "transportation", "ticket" })
    private Seat assignedSeat;

    private BigDecimal seatPriceModifier;
    private BigDecimal totalPrice;

    @Column(name = "booking_date")
    @Builder.Default
    private LocalDateTime bookingDate = LocalDateTime.now();

    @Column(name = "is_paid")
    @JsonProperty("paid")
    @Builder.Default
    private boolean isPaid = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TicketStatus ticketStatus = TicketStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GenericStatus approvalStatus = GenericStatus.PENDING;

    @Column(name = "has_meal_plan", nullable = false)
    @Builder.Default
    private boolean hasMealPlan = false;

    @ManyToMany
    @JoinTable(name = "ticket_meals_selection", joinColumns = @JoinColumn(name = "ticket_id"), inverseJoinColumns = @JoinColumn(name = "meal_plan_id"))
    private List<MealPlan> selectedMeals;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    @JsonIgnoreProperties({ "tour", "user" })
    private TourReservation reservation;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", updatable = false)
    @JsonIgnoreProperties({ "password" })
    private User createdBy;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    @JsonIgnoreProperties({ "password" })
    private User updatedBy;

    private BigDecimal basePrice;
    private BigDecimal discountPrice;

    @Column(columnDefinition = "TEXT")
    private String qrCode;

    @Column(columnDefinition = "TEXT")
    private String barcode;

}
