package com.hisabkitab.domain;

/** Who can sign in. Employees are not login users. */
public enum Role {
    /** Full control including employee/employer setup and voiding entries. */
    OWNER,
    /** Day to day recording: attendance, advances, repayments. */
    MANAGER
}
