package com.hisabkitab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A dated wage rate. Rates change; without history, reposting an old month
 * would silently pay the new rate for days worked under the old one.
 */
@Entity
@Table(name = "wage_rates")
@Getter
@Setter
@NoArgsConstructor
public class WageRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "daily_rate", nullable = false, precision = 14, scale = 2)
    private BigDecimal dailyRate;

    /** Applies from this date until the next rate's effectiveFrom. */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public WageRate(Employee employee, BigDecimal dailyRate, LocalDate effectiveFrom, String note) {
        this.employee = employee;
        this.dailyRate = dailyRate;
        this.effectiveFrom = effectiveFrom;
        this.note = note;
    }
}
