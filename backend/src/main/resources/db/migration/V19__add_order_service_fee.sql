-- Service fee passed through to iFood on orders imported from Anota.AI (iFood sales channel).
-- It is included in orders.total_value but is not restaurant revenue, so it must be excluded
-- from profit/margin. Nullable: existing orders and manual/iFood-direct orders keep NULL (= 0).
ALTER TABLE orders ADD COLUMN service_fee NUMERIC(19, 4);
