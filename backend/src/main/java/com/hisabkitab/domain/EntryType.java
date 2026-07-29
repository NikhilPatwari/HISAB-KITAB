package com.hisabkitab.domain;

/**
 * Ledger entry kinds, signed from the employee's point of view.
 * <p>
 * A positive balance means the organization owes the employee (unpaid wages).
 * A negative balance means the employee owes the organization (outstanding advance).
 */
public enum EntryType {

    /** Cash lent to the employee. */
    ADVANCE(-1, "Advance given"),

    /** Employer paid a third party on the employee's behalf — hospital, shop, school fee. */
    EXPENSE_ON_BEHALF(-1, "Paid on behalf"),

    /** Wages handed over in cash, settling what the organization owed. */
    PAYOUT(-1, "Wage paid out"),

    /** Penalty, damage recovery or agreed cut. */
    DEDUCTION(-1, "Deduction"),

    /** Wages earned for a period. Normally created by a wage run. */
    WAGE(+1, "Wages earned"),

    /** Piece-rate earnings for units of work completed. Also created by a wage run. */
    PIECE_WORK(+1, "Work completed"),

    /** Extra payment on top of wages. */
    BONUS(+1, "Bonus"),

    /** Employee handed cash back against an advance. */
    REPAYMENT(+1, "Repayment received"),

    /** Manual correction. The caller decides the sign. */
    ADJUSTMENT(0, "Adjustment");

    private final int sign;
    private final String label;

    EntryType(int sign, String label) {
        this.sign = sign;
        this.label = label;
    }

    /** -1, +1, or 0 when the caller supplies the sign. */
    public int sign() {
        return sign;
    }

    public String label() {
        return label;
    }

    /** True when the entry increases what the employee owes. */
    public boolean isDebit() {
        return sign < 0;
    }
}
