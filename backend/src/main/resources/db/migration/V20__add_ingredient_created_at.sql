-- Creation timestamp for ingredients, used by the ingredients list to filter by
-- creation date. Nullable: rows created before this column existed have unknown
-- creation dates and stay NULL (no backfill).
ALTER TABLE ingredients ADD COLUMN created_at timestamp(6);
