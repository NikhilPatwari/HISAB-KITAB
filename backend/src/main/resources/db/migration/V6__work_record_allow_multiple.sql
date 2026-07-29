-- Reverses the one-per-day rule from V5.
--
-- A worker can legitimately deliver more than once on the same job in a day —
-- a morning batch and an afternoon batch. Each entry now stands on its own,
-- keeping the quantity, price and note it was agreed at, and the day's pay is
-- the sum of them. Correcting a mistaken entry means deleting that entry.

DROP INDEX IF EXISTS uq_work_records_day;
