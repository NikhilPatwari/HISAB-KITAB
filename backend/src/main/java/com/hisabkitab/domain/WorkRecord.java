package com.hisabkitab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Units of work one person completed on one day: "Ramesh, cotton picking,
 * 34 kg, 12 July".
 * <p>
 * The unit price is snapshotted at entry rather than read through to the task,
 * because piece work is agreed at a price for the job — repricing history when
 * next season's rate is set would be wrong.
 */
@Entity
@Table(name = "work_records")
@Getter
@Setter
@NoArgsConstructor
public class WorkRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_task_id", nullable = false)
    private WorkTask workTask;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    /** Scale 3 so half kilos and part-bighas are recordable. */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    /** The task's price at the moment this was entered. */
    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    /** quantity × unitPrice, stored so a sum never has to re-multiply. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
