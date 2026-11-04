package com.server.server.models.support;

import com.server.server.models.base.BaseEntity;

import com.server.server.models.user.User;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.server.server.enums.notification.ReferenceType;
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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@Entity
@Table(name = "generic_event_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenericEventLog extends BaseEntity {

    @Column(nullable = false)
    private Integer referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReferenceType referenceType;

    @Column(nullable = false)
    private String action; // e.g., "CREATED", "STATUS_CHANGED", "ASSIGNED"

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actor_id", nullable = false)
    @JsonIgnoreProperties({"password", "agency", "agencyBranch", "hibernateLazyInitializer"})
    private User actor;

    @JsonProperty("type")
    public String getType() {
        return "EVENT";
    }

}
