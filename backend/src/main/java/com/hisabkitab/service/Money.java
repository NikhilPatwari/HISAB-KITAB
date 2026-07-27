package com.hisabkitab.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Every amount in the system is scale 2, rounded half up. */
public final class Money {

    public static final BigDecimal ZERO = scale(BigDecimal.ZERO);

    private Money() {
    }

    public static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? ZERO : scale(value);
    }
}
