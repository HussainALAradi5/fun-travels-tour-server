package com.server.server.models.tourmanagement;

import com.server.server.models.base.BaseEntity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "seats")
public class Seat extends BaseEntity {

    @Column(nullable = false)
    private String seatCode;

    @Enumerated(EnumType.STRING)
    private ChairType chairType = ChairType.STANDARD;

    @Enumerated(EnumType.STRING)
    private SeatStatus status = SeatStatus.AVAILABLE;

    private Double seatPriceModifier = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transportation_id")
    @JsonBackReference(value = "transportation-seats")
    private Transportation transportation;
}
