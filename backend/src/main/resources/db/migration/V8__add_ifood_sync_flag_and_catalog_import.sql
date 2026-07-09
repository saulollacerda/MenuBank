-- Per-merchant opt-in for iFood order polling and catalog import tracking.
alter table merchants
    add column ifood_order_sync_enabled boolean not null default false,
    add column ifood_catalog_imported_at timestamp;

-- Merchants already connected keep syncing (behavior before the flag existed).
update merchants
set ifood_order_sync_enabled = true
where ifood_merchant_id is not null;
