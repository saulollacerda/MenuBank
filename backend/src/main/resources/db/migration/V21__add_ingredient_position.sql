-- Manual ordering for the ingredients list. `position` is a zero-based, per-merchant
-- sequential index that becomes the default listing order (drag-and-drop reordering).
-- Backfill assigns positions to existing rows following the previous default listing
-- order (name ASC, id ASC as a stable tie-breaker) so every merchant starts contiguous.
-- New rows get max(position)+1 for the merchant on create (see IngredientService).
ALTER TABLE ingredients ADD COLUMN position integer;

WITH ordered AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY merchant_id ORDER BY name ASC, id ASC) - 1 AS seq
    FROM ingredients
)
UPDATE ingredients i
SET position = o.seq
FROM ordered o
WHERE i.id = o.id;
