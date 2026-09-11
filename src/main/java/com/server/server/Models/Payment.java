package com.server.server.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.math.BigDecimal; 
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.server.server.enums.Payment.PaymentMethod;
import com.server.server.enums.Payment.PaymentStatus;
import com.server.server.models.tourmanagement.TourReservation;

import lombok.*;

@Entity
@Table(name = "payments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String transactionId;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    // REFACTORED: Double -> BigDecimal
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;
    
    private String currency;
    private LocalDateTime paymentDate;

    // REFACTORED: Changed to ManyToOne. A reservation might have a deposit payment, and a final payment later.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private TourReservation reservation;

    // NEW: Link to the internal ledger transactions
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL)
    @JsonIgnore
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();
}
