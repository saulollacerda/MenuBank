-- Order imports (iFood/Anota AI) match options against ingredients by canonical name.
-- Legacy rows with a null/blank canonical_name never match and silently drop the
-- ingredient cost from imported orders. Backfill mirrors V5 (products).
update ingredients
set canonical_name = lower(
    regexp_replace(
        trim(translate(name,
            'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ',
            'aaaaaeeeeiiiiooooouuuucAAAAAEEEEIIIIOOOOOUUUUC')),
        '\s+', ' ', 'g')
)
where canonical_name is null or canonical_name = '';

create index idx_ingredients_merchant_canonical_name
    on ingredients (merchant_id, canonical_name);
