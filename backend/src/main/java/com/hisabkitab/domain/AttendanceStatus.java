package com.hisabkitab.domain;

import java.math.BigDecimal;

/**
 * Attendance is recorded by exception. A working day with no attendance row is
 * a full day worked, so this enum only covers the deviations.
 */
public enum AttendanceStatus {

    /** Did not work. Earns nothing. */
    ABSENT(new BigDecimal("0.0")),

    /** Worked part of the day. Earns half the daily rate. */
    HALF_DAY(new BigDecimal("0.5")),

    /** Away but still paid — festival, sick leave granted by the employer. */
    PAID_LEAVE(BigDecimal.ONE),

    /** Worked a full day plus extra. Earns one and a half times the rate. */
    OVERTIME(new BigDecimal("1.5"));

    private final BigDecimal dayFraction;

    AttendanceStatus(BigDecimal dayFraction) {
        this.dayFraction = dayFraction;
    }

    /** Multiplier applied to the daily wage rate for this day. */
    public BigDecimal dayFraction() {
        return dayFraction;
    }
}
