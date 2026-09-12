package com.server.server.models.tourmanagement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.server.server.enums.GenericStatus;
import com.server.server.models.Payment;
import com.server.server.models.Transaction;
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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tour_reservations")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TourReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

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

    // NEW: A reservation can have multiple ledger transactions (e.g., a payment, then a partial refund)
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Transaction> transactions = new ArrayList<>();

    // NEW: A reservation can have multiple external payments (e.g., Deposit paid now, remainder paid later)
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Payment> payments = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime bookingDate;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;
}
