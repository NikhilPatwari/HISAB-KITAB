package com.hisabkitab.domain;

/**
 * How a worker earns. This is the single place the three arrangements are
 * defined, so the wage engine and the attendance screens can ask rather than
 * re-derive the rules.
 */
public enum EmployeeType {

    /**
     * On the books every working day at a daily rate. Presence is assumed and
     * only absences are marked — the original arrangement.
     */
    PERMANENT("Permanent", "Present by default, paid a daily wage"),

    /**
     * Paid a daily rate, but only for days explicitly marked present. A day with
     * no attendance row earns nothing, which is the inverse of PERMANENT.
     */
    TEMPORARY("Temporary", "Only paid for days marked present"),

    /**
     * Paid per unit of work completed rather than per day. Does not appear on
     * the attendance roster at all.
     */
    CONTRACT("Contract", "Paid per unit of work done");

    private final String label;
    private final String description;

    EmployeeType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    /** True when the worker earns a daily rate at all. */
    public boolean usesDailyWage() {
        return this != CONTRACT;
    }

    /** True when a working day with no attendance row counts as worked. */
    public boolean presentByDefault() {
        return this == PERMANENT;
    }

    /** True when the worker belongs on the attendance roster. */
    public boolean tracksAttendance() {
        return this != CONTRACT;
    }
}
