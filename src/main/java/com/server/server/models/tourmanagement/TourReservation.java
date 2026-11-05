package com.server.server.models.tourmanagement;

import com.server.server.models.base.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.server.server.enums.common.GenericStatus;
import com.server.server.models.payment.Payment;
import com.server.server.models.payment.Transaction;
import com.server.server.models.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tour_reservations")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TourReservation extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String reservationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    @JsonIgnoreProperties({ "reservations", "tickets", "agency", "transportation" })
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    // FIX: Added "account" to prevent infinite JSON recursion with the new Wallet feature
    @JsonIgnoreProperties({ "password", "agency", "agencyBranch", "account" })
    private User user;

    private Integer requestedSlots;

    // REFACTORED: Changed from Double to BigDecimal to prevent financial rounding errors
    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private GenericStatus status = GenericStatus.PENDING;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Ticket> tickets = new ArrayList<>();

    /** Ledger entries for this booking, such as its payment and later refunds. */
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Transaction> transactions = new ArrayList<>();

    /** Payments made for this booking. */
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Payment> payments = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime bookingDate;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;
}
