-- Valor pago pelo cliente por cada adicional (subItem da Anota.AI), copiado literalmente
-- do payload. Independente do custo de produção (cost_per_unit), que vem do catálogo local.
-- sale_price_per_unit = subItem.price  |  sale_price_total = subItem.total
-- Colunas nulas: extras de pedidos manuais/iFood não têm preço, e pedidos importados
-- antes desta migração permanecem sem valor (não há backfill).
alter table order_item_extra_ingredients add column sale_price_per_unit numeric(19, 4);
alter table order_item_extra_ingredients add column sale_price_total numeric(19, 4);
