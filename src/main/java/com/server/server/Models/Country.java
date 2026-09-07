package com.server.server.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "countries")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String famousName;

    @Column(columnDefinition = "TEXT")
    private String officialName;

    @Column(length = 2, unique = true)
    private String countryCode;

    @Column(length = 10)
    private String dialCode;

    @Column
    private Integer mobileNumberLength;

    private String flagPngUrl;

    private String flagSvgUrl;
}