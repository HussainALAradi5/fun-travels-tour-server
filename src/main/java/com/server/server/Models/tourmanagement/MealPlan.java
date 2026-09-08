package com.server.server.models.tourmanagement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.server.server.enums.GenericStatus;
import com.server.server.models.Agency.Agency;

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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "meal_plans")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class MealPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    @NotBlank(message = "Meal Name is required")
    private String mealName;

    @Column(nullable = false)
    @NotNull(message = "Meal Price is required")
    private Double mealPrice;

    @Column(columnDefinition = "TEXT")
    private String mealDescription;

    private boolean isVegetarian = false;
    private boolean isVegan = false;
    private boolean isGlutenFree = false;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    private GenericStatus status = GenericStatus.ACTIVE;

    // FIX: Added the missing Agency relationship so the Repository can filter by Agency
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    @JsonIgnoreProperties({"owner", "branches", "tours", "transportations"})
    private Agency agency;
}
