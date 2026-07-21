-- Last write timestamp for an order, maintained by Hibernate (@UpdateTimestamp) on insert
-- and on every edit. Existing rows are backfilled with date_time (their creation instant),
-- which is the closest known value: an order never edited was last written when it was created.
ALTER TABLE orders ADD COLUMN updated_at TIMESTAMP;

UPDATE orders SET updated_at = date_time WHERE updated_at IS NULL;
