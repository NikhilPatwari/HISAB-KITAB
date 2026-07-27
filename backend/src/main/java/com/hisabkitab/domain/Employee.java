package com.hisabkitab.domain;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A farm worker. Never a login user. */
@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** Optional short code the farm already uses on paper. */
    @Column(length = 32)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 32)
    private String phone;

    @Column(length = 160)
    private String village;

    /**
     * Current rate, denormalised for listing screens. The authoritative history
     * lives in {@link WageRate} and is what wage runs actually read.
     */
    @Column(name = "daily_wage_rate", nullable = false, precision = 14, scale = 2)
    private BigDecimal dailyWageRate = BigDecimal.ZERO;

    @Column(name = "joined_on", nullable = false)
    private LocalDate joinedOn;

    /** Last working day, once the employee leaves. */
    @Column(name = "exited_on")
    private LocalDate exitedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /** True when the employee was on the books on the given day. */
    public boolean isEmployedOn(LocalDate date) {
        if (date.isBefore(joinedOn)) {
            return false;
        }
        return exitedOn == null || !date.isAfter(exitedOn);
    }
}
