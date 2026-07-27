package com.hisabkitab.repository;

import java.math.BigDecimal;

/** Projection for the per-employee balance rollup used by list screens. */
public interface EmployeeBalanceRow {

    Long getEmployeeId();

    BigDecimal getBalance();
}
