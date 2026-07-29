-- Three ways of earning, where there was previously only one.
--
--   PERMANENT  present by default, paid a daily wage  (the existing behaviour)
--   TEMPORARY  paid a daily wage, but only for days marked present
--   CONTRACT   paid per unit of work done, no attendance at all
--
-- Every existing worker was permanent, so the default backfills them correctly.

ALTER TABLE employees
    ADD COLUMN employee_type VARCHAR(16) NOT NULL DEFAULT 'PERMANENT';

ALTER TABLE employees
    ADD CONSTRAINT ck_employees_type
        CHECK (employee_type IN ('PERMANENT', 'TEMPORARY', 'CONTRACT'));

-- Contract workers have no daily rate, so the wage engine must never be asked
-- for one. Listing screens filter on this often enough to earn an index.
CREATE INDEX idx_employees_org_type ON employees (organization_id, employee_type);
