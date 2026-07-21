-- Backfill for orders whose total lost the delivery/service fee when they were edited.
--
-- Until the fix in OrderService.update(), saving an imported order recomputed total_value as
-- the plain sum of its item lines, dropping delivery_fee and service_fee from the total while
-- both kept being subtracted from the profit. Such an order is recognisable: it carries fees
-- and its total equals the sum of its lines, which can never happen on an untouched imported
-- order (the partner total always includes the fees).
--
-- Restores total_value = total_value + delivery_fee + service_fee and recomputes
-- estimated_profit with the same formula as OrderCalculations.calculateEstimatedProfit:
--     subtotal = total_value - delivery_fee - service_fee   (= the stored, item-only total)
--     profit   = subtotal - total_cost - subtotal * fee_rate / 100
--
-- NOT recoverable here: the sale price of paid extras. Editing rebuilt the extra rows with
-- sale_price_total = NULL, so that revenue is gone from the database — only the recent orders
-- still present in external_order_raw_payload could be restored, and that is a separate job.
--
-- The previous values are kept in orders_total_backfill_v24 so the change can be audited or
-- reverted.

CREATE TABLE IF NOT EXISTS orders_total_backfill_v24 (
    order_id             UUID PRIMARY KEY REFERENCES orders (id),
    old_total_value      NUMERIC(38, 2) NOT NULL,
    old_estimated_profit NUMERIC(38, 2) NOT NULL,
    new_total_value      NUMERIC(38, 2) NOT NULL,
    new_estimated_profit NUMERIC(38, 2) NOT NULL,
    backfilled_at        TIMESTAMP      NOT NULL DEFAULT NOW()
);

WITH affected AS (
    SELECT o.id,
           o.total_value,
           o.estimated_profit,
           COALESCE(o.delivery_fee, 0) AS delivery_fee,
           COALESCE(o.service_fee, 0)  AS service_fee,
           COALESCE(o.total_cost, 0)   AS total_cost,
           COALESCE(f.fee_rate, 0)     AS fee_rate
    FROM orders o
             LEFT JOIN fees f ON f.id = o.fee_id
    WHERE o.origin IN ('ANOTA_AI', 'IFOOD')
      AND COALESCE(o.delivery_fee, 0) + COALESCE(o.service_fee, 0) > 0
      AND o.total_value = (
          SELECT COALESCE(SUM(oi.unit_price * oi.quantity), 0)
                 + COALESCE((SELECT SUM(e.sale_price_total)
                             FROM order_item_extra_ingredients e
                             WHERE e.order_item_id IN (SELECT id FROM order_items WHERE order_id = o.id)), 0)
          FROM order_items oi
          WHERE oi.order_id = o.id
      )
),
recomputed AS (
    SELECT id,
           total_value      AS old_total_value,
           estimated_profit AS old_estimated_profit,
           ROUND(total_value + delivery_fee + service_fee, 2) AS new_total_value,
           -- subtotal after the fix == the item-only total stored today
           ROUND(total_value - total_cost - ROUND(total_value * fee_rate / 100, 4), 2) AS new_estimated_profit
    FROM affected
)
INSERT INTO orders_total_backfill_v24 (order_id, old_total_value, old_estimated_profit,
                                       new_total_value, new_estimated_profit)
SELECT id, old_total_value, old_estimated_profit, new_total_value, new_estimated_profit
FROM recomputed
ON CONFLICT (order_id) DO NOTHING;

UPDATE orders o
SET total_value      = b.new_total_value,
    estimated_profit = b.new_estimated_profit
FROM orders_total_backfill_v24 b
WHERE b.order_id = o.id
  AND o.total_value = b.old_total_value;
