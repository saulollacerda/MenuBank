-- Tracks where each catalog record came from: MENUBANK (created in the app),
-- ANOTA_AI or IFOOD (imported).
alter table products
    add column origin varchar(20) not null default 'MENUBANK';
alter table categories
    add column origin varchar(20) not null default 'MENUBANK';

alter table products
    add constraint products_origin_check
        check (origin in ('MENUBANK', 'ANOTA_AI', 'IFOOD'));
alter table categories
    add constraint categories_origin_check
        check (origin in ('MENUBANK', 'ANOTA_AI', 'IFOOD'));

-- Backfill: before this migration only the Anota.AI catalog sync populated
-- external_id (manual creation never sets it and the iFood import ships together
-- with this column), so external_id is a reliable marker of Anota.AI imports.
update products set origin = 'ANOTA_AI' where external_id is not null;
update categories set origin = 'ANOTA_AI' where external_id is not null;
