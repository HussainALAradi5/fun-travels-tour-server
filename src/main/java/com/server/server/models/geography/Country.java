package com.server.server.models.geography;

import com.server.server.models.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "countries")
public class Country extends BaseEntity {

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
