package com.server.server.models.support;

import com.server.server.models.base.BaseEntity;

import com.server.server.models.user.User;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.server.server.enums.support.UserRequestStatus;
import com.server.server.enums.support.UserRequestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "user_requests")
public class UserRequest extends BaseEntity {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Request type is required")
    @Enumerated(EnumType.STRING)
    private UserRequestType type;

    @Enumerated(EnumType.STRING)
    private UserRequestStatus status = UserRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({ "password", "resetToken", "resetTokenExpiry", "base64Image", "agency", "agencyBranch" })
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    @JsonIgnoreProperties({ "password", "resetToken", "resetTokenExpiry", "base64Image" })
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solved_by_id")
    @JsonIgnoreProperties({ "password", "resetToken", "resetTokenExpiry", "base64Image" })
    private User solvedBy;

    @Column(name = "solved_at")
    private LocalDateTime solvedAt;

}
