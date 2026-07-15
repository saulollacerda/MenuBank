-- Normalize the merchants table: move the iFood and Anota.AI integration columns into
-- their own 1:1 tables (ifood_integration, anotaai_integration). The OAuth app token
-- stays in ifood_app_token (V4) — it is app-level, not per merchant.
--
-- Column names/types mirror the JPA mapping (IfoodIntegration / AnotaAiIntegration),
-- the same way V8/V13 kept Flyway in lockstep with Hibernate. Data is copied only for
-- merchants that actually have a value (COPY guard below), so all-default rows are not
-- materialized — matching the create-on-demand behavior of the convenience accessors on
-- the Merchant entity. Every copied row round-trips losslessly.

create table ifood_integration (
    id                        uuid      primary key default gen_random_uuid(),
    merchant_id               uuid      not null unique references merchants(id),
    ifood_merchant_id         varchar(255),
    ifood_authorized_at       timestamp,
    ifood_order_sync_enabled  boolean   not null default false,
    ifood_catalog_imported_at timestamp
);

create table anotaai_integration (
    id               uuid primary key default gen_random_uuid(),
    merchant_id      uuid not null unique references merchants(id),
    anota_ai_api_key text
);

-- Copy existing iFood state for any merchant that has ever connected or toggled sync.
insert into ifood_integration (
    merchant_id, ifood_merchant_id, ifood_authorized_at,
    ifood_order_sync_enabled, ifood_catalog_imported_at)
select id, ifood_merchant_id, ifood_authorized_at,
       ifood_order_sync_enabled, ifood_catalog_imported_at
from merchants
where ifood_merchant_id is not null
   or ifood_authorized_at is not null
   or ifood_order_sync_enabled = true
   or ifood_catalog_imported_at is not null;

-- Copy existing Anota.AI keys.
insert into anotaai_integration (merchant_id, anota_ai_api_key)
select id, anota_ai_api_key
from merchants
where anota_ai_api_key is not null;

-- Drop the now-normalized columns from merchants.
alter table merchants
    drop column ifood_merchant_id,
    drop column ifood_authorized_at,
    drop column ifood_order_sync_enabled,
    drop column ifood_catalog_imported_at,
    drop column anota_ai_api_key;
