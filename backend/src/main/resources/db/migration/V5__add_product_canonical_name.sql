alter table products add column canonical_name varchar(255);

-- Backfill: normalize existing names (lowercase, strip accents, collapse whitespace).
-- unaccent() is not assumed available; translate() covers pt-BR accented characters.
update products
set canonical_name = lower(
    regexp_replace(
        trim(translate(name,
            'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ',
            'aaaaaeeeeiiiiooooouuuucAAAAAEEEEIIIIOOOOOUUUUC')),
        '\s+', ' ', 'g')
);

create index idx_products_merchant_canonical_name
    on products (merchant_id, canonical_name);
