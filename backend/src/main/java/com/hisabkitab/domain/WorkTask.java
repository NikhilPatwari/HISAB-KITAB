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

/**
 * A piece-rate job: "Cotton picking, Field 3, per kg, ₹12".
 * <p>
 * A reusable definition rather than a per-worker assignment — any number of
 * workers log units against it, across any number of days, season after season.
 * <p>
 * {@link #pricePerUnit} is the <em>current</em> price. Each {@link WorkRecord}
 * snapshots the price it was entered at, so changing this never reprices work
 * already done.
 */
@Entity
@Table(name = "work_tasks")
@Getter
@Setter
@NoArgsConstructor
public class WorkTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 160)
    private String name;

    /** Where the work happens — a field, a plot, a shed. */
    @Column(length = 160)
    private String location;

    /** What is counted: kg, bigha, tree, sack, hour. */
    @Column(name = "unit_of_work", nullable = false, length = 32)
    private String unitOfWork;

    @Column(name = "price_per_unit", nullable = false, precision = 14, scale = 2)
    private BigDecimal pricePerUnit = BigDecimal.ZERO;

    @Column(length = 1000)
    private String notes;

    /** Archived rather than deleted, so past work records keep resolving. */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
