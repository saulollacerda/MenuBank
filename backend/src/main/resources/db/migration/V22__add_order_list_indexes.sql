-- Indexes for the paged order list (GET /api/orders), the slowest screen in production.
--
-- Postgres does NOT create indexes for foreign keys, and none of these columns had one.
-- Every page load therefore sequential-scanned `orders` (filter merchant_id + sort
-- date_time DESC) and then sequential-scanned `order_items`, its extra ingredients and its
-- excluded-includes collection table to batch-load the item graph of the 20 rows on screen.
--
-- Measured with EXPLAIN ANALYZE on a local copy of the schema holding 30k orders /
-- 60k order items (page of 20, single merchant):
--   list page query   11.9 ms -> 0.2 ms   (Seq Scan on orders   -> Index Scan)
--   count query        7.9 ms -> 2.0 ms   (Seq Scan on orders   -> Bitmap Index Scan)
--   item batch fetch  21.9 ms -> 0.3 ms   (Seq Scan on order_items -> Index Scan)
-- The gap grows with the table, since every scan reads the whole table.

-- Filter (merchant_id) + sort (date_time DESC) of the list, and the same pair used by the
-- count query, the status counters and every report that scans a merchant's period.
CREATE INDEX IF NOT EXISTS idx_orders_merchant_date_time
    ON orders (merchant_id, date_time DESC);

-- Batch fetch of Order.items ("... where order_id in (?, ?, ...)").
CREATE INDEX IF NOT EXISTS idx_order_items_order
    ON order_items (order_id);

-- Batch fetch of OrderItem.extraIngredients.
CREATE INDEX IF NOT EXISTS idx_order_item_extra_ingredients_item
    ON order_item_extra_ingredients (order_item_id);

-- OrderItem.excludedIncludeIds needs no index: its collection table is keyed by
-- PRIMARY KEY (order_item_id, include_id), whose leading column already serves the
-- batch fetch.

-- Ficha técnica of the products on the page, loaded in one shot per page
-- ("... where product_id in (?, ?, ...)").
CREATE INDEX IF NOT EXISTS idx_includes_product
    ON includes (product_id);
