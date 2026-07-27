package com.hisabkitab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/** The farm. Every other record hangs off exactly one of these. */
@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "INR";

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone = "Asia/Kolkata";

    /** Unpaid weekly offs. Empty means every calendar day is a working day. */
    @Convert(converter = DayOfWeekSetConverter.class)
    @Column(name = "weekly_off_days", nullable = false, length = 120)
    private Set<DayOfWeek> weeklyOffDays = EnumSet.noneOf(DayOfWeek.class);

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isWorkingDay(java.time.LocalDate date) {
        return !weeklyOffDays.contains(date.getDayOfWeek());
    }
}
