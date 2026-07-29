-- Piece-rate work: reusable job definitions, and the units each worker completed.

CREATE TABLE work_tasks (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name            VARCHAR(160)   NOT NULL,
    location        VARCHAR(160),
    -- What is counted: kg, bigha, tree, sack, hour.
    unit_of_work    VARCHAR(32)    NOT NULL,
    -- The current price. Each work_record snapshots the price it was entered
    -- at, so changing this never reprices work already done.
    price_per_unit  NUMERIC(14, 2) NOT NULL DEFAULT 0,
    notes           VARCHAR(1000),
    active          BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_work_tasks_price CHECK (price_per_unit >= 0)
);
CREATE INDEX idx_work_tasks_org ON work_tasks (organization_id, active);

CREATE TABLE work_records (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    employee_id     BIGINT         NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    -- Restricted, not cascaded: a task with recorded work must be archived
    -- rather than deleted, so the history stays readable.
    work_task_id    BIGINT         NOT NULL REFERENCES work_tasks (id) ON DELETE RESTRICT,
    work_date       DATE           NOT NULL,
    quantity        NUMERIC(14, 3) NOT NULL,
    unit_price      NUMERIC(14, 2) NOT NULL,
    amount          NUMERIC(14, 2) NOT NULL,
    note            VARCHAR(500),
    created_by      BIGINT         REFERENCES app_users (id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_work_records_quantity CHECK (quantity > 0),
    CONSTRAINT ck_work_records_price CHECK (unit_price >= 0)
);
CREATE INDEX idx_work_records_employee_date ON work_records (employee_id, work_date);
CREATE INDEX idx_work_records_org_date ON work_records (organization_id, work_date);
CREATE INDEX idx_work_records_task ON work_records (work_task_id);

-- Piece-work earnings post as their own entry type so a statement distinguishes
-- "Wages for July - 26 days" from "Cotton picking - 340 kg".
ALTER TABLE ledger_entries
    DROP CONSTRAINT ck_ledger_type;

ALTER TABLE ledger_entries
    ADD CONSTRAINT ck_ledger_type CHECK (entry_type IN (
        'ADVANCE', 'EXPENSE_ON_BEHALF', 'PAYOUT', 'DEDUCTION',
        'WAGE', 'PIECE_WORK', 'BONUS', 'REPAYMENT', 'ADJUSTMENT'));
