-- Hisab Kitab :: initial schema
-- Money is NUMERIC(14,2). Dates that represent a business day are DATE (no timezone).
-- Audit instants are TIMESTAMPTZ.

CREATE TABLE organizations (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(160) NOT NULL,
    currency_code   VARCHAR(3)   NOT NULL DEFAULT 'INR',
    time_zone       VARCHAR(64)  NOT NULL DEFAULT 'Asia/Kolkata',
    -- Comma separated java.time.DayOfWeek names that are unpaid weekly offs.
    -- Empty means every calendar day counts as a working day.
    weekly_off_days VARCHAR(120) NOT NULL DEFAULT '',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- A person inside the organization whose own money funds the advances.
CREATE TABLE employers (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name            VARCHAR(160) NOT NULL,
    phone           VARCHAR(32),
    notes           VARCHAR(1000),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_employers_org ON employers (organization_id);

-- Login accounts. Only owners/managers sign in; employees do not.
CREATE TABLE app_users (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    employer_id     BIGINT       REFERENCES employers (id) ON DELETE SET NULL,
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(120) NOT NULL,
    display_name    VARCHAR(160) NOT NULL,
    role            VARCHAR(16)  NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_users_username UNIQUE (username),
    CONSTRAINT ck_app_users_role CHECK (role IN ('OWNER', 'MANAGER'))
);
CREATE INDEX idx_app_users_org ON app_users (organization_id);

CREATE TABLE employees (
    id                BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT        NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    code              VARCHAR(32),
    name              VARCHAR(160)  NOT NULL,
    phone             VARCHAR(32),
    village           VARCHAR(160),
    daily_wage_rate   NUMERIC(14, 2) NOT NULL DEFAULT 0,
    joined_on         DATE          NOT NULL,
    exited_on         DATE,
    status            VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    notes             VARCHAR(1000),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_employees_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_employees_exit CHECK (exited_on IS NULL OR exited_on >= joined_on),
    CONSTRAINT uq_employees_code UNIQUE (organization_id, code)
);
CREATE INDEX idx_employees_org_status ON employees (organization_id, status);

-- Wage rate history so that reposting an old month uses the rate that applied then.
CREATE TABLE wage_rates (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT         NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    daily_rate      NUMERIC(14, 2) NOT NULL,
    effective_from  DATE           NOT NULL,
    note            VARCHAR(255),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_wage_rates_from UNIQUE (employee_id, effective_from),
    CONSTRAINT ck_wage_rates_positive CHECK (daily_rate >= 0)
);
CREATE INDEX idx_wage_rates_employee ON wage_rates (employee_id, effective_from);

-- Exception-based attendance: a row exists only when the employee was NOT
-- fully present. No row for a working day means a full day was worked.
CREATE TABLE attendance (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    employee_id     BIGINT       NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    work_date       DATE         NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    note            VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_attendance_day UNIQUE (employee_id, work_date),
    CONSTRAINT ck_attendance_status CHECK (status IN ('ABSENT', 'HALF_DAY', 'PAID_LEAVE', 'OVERTIME'))
);
CREATE INDEX idx_attendance_org_date ON attendance (organization_id, work_date);
CREATE INDEX idx_attendance_employee_date ON attendance (employee_id, work_date);

-- Monthly wage posting. Draft is a preview; posting writes WAGE ledger entries
-- and locks the month against a second posting.
CREATE TABLE wage_runs (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    period_start    DATE           NOT NULL,
    period_end      DATE           NOT NULL,
    status          VARCHAR(16)    NOT NULL DEFAULT 'POSTED',
    total_amount    NUMERIC(14, 2) NOT NULL DEFAULT 0,
    posted_at       TIMESTAMPTZ,
    posted_by       BIGINT         REFERENCES app_users (id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_wage_runs_status CHECK (status IN ('POSTED', 'VOIDED')),
    CONSTRAINT ck_wage_runs_period CHECK (period_end >= period_start)
);

-- A month may be posted only once, but a voided run leaves the month free to be
-- corrected and posted again, so the uniqueness is scoped to POSTED rows.
CREATE UNIQUE INDEX uq_wage_runs_posted_period
    ON wage_runs (organization_id, period_start)
    WHERE status = 'POSTED';

-- The ledger. One row per money movement, signed from the employee's point of
-- view: positive means the organization owes the employee, negative means the
-- employee owes the organization.
CREATE TABLE ledger_entries (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    employee_id     BIGINT         NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    employer_id     BIGINT         REFERENCES employers (id) ON DELETE SET NULL,
    wage_run_id     BIGINT         REFERENCES wage_runs (id) ON DELETE SET NULL,
    entry_type      VARCHAR(24)    NOT NULL,
    -- Always stored as a positive magnitude; the sign comes from entry_type
    -- (or from signed_amount for ADJUSTMENT).
    amount          NUMERIC(14, 2) NOT NULL,
    signed_amount   NUMERIC(14, 2) NOT NULL,
    entry_date      DATE           NOT NULL,
    note            VARCHAR(500),
    voided          BOOLEAN        NOT NULL DEFAULT FALSE,
    voided_at       TIMESTAMPTZ,
    voided_by       BIGINT         REFERENCES app_users (id) ON DELETE SET NULL,
    created_by      BIGINT         REFERENCES app_users (id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_ledger_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_ledger_type CHECK (entry_type IN (
        'ADVANCE', 'EXPENSE_ON_BEHALF', 'PAYOUT', 'DEDUCTION',
        'WAGE', 'BONUS', 'REPAYMENT', 'ADJUSTMENT'))
);
CREATE INDEX idx_ledger_employee_date ON ledger_entries (employee_id, entry_date, id);
CREATE INDEX idx_ledger_org_date ON ledger_entries (organization_id, entry_date);
CREATE INDEX idx_ledger_employer ON ledger_entries (employer_id);
CREATE INDEX idx_ledger_wage_run ON ledger_entries (wage_run_id);
