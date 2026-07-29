-- One tally per worker, per task, per day.
--
-- Without this, logging the same day twice created two rows and paid twice.
-- Recording is now an overwrite: the number entered is the day's total for that
-- worker on that job, not an addition to it.

-- Collapse any duplicates already recorded, keeping the most recent entry,
-- which is the one the constraint would have left standing.
DELETE FROM work_records a
    USING work_records b
WHERE a.employee_id = b.employee_id
  AND a.work_task_id = b.work_task_id
  AND a.work_date = b.work_date
  AND a.id < b.id;

CREATE UNIQUE INDEX uq_work_records_day
    ON work_records (employee_id, work_task_id, work_date);
