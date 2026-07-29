-- Temporary workers earn nothing on a day with no attendance row, so presence
-- has to be recordable rather than assumed. Permanent workers are unaffected:
-- for them a PRESENT row is simply redundant with the default.

ALTER TABLE attendance
    DROP CONSTRAINT ck_attendance_status;

ALTER TABLE attendance
    ADD CONSTRAINT ck_attendance_status
        CHECK (status IN ('PRESENT', 'ABSENT', 'HALF_DAY', 'PAID_LEAVE', 'OVERTIME'));
