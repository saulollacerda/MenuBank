-- Raw JSON payloads of imported external orders (Anota.AI / iFood), kept for
-- 3 days so financial inconsistencies can be audited against the original data.
-- Purged daily by ExternalOrderRawPayloadCleanupScheduler.
create table external_order_raw_payload (
    id uuid not null primary key,
    merchant_id uuid not null references merchants (id) on delete cascade,
    origin varchar(20) not null,
    external_order_id varchar(255) not null,
    payload jsonb not null,
    created_at timestamp(6) not null
);

-- Retention purge scans by age
create index idx_external_order_raw_payload_created_at
    on external_order_raw_payload (created_at);

-- Audit lookup: find the payload of a specific imported order
create index idx_external_order_raw_payload_merchant_order
    on external_order_raw_payload (merchant_id, external_order_id);
