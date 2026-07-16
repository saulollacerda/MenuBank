-- Ficha do PEDIDO: insumos consumidos UMA vez por pedido, independentemente da quantidade
-- de itens (sacola de entrega, guardanapo, cupom). Complementa a ficha técnica do produto
-- (tabela includes), que é por item e multiplicada pela quantidade — correto para copo/colher,
-- errado para a sacola: 2 copos saem numa sacola só.
--
-- Retrocompatível: nenhum lojista tem ficha do pedido configurada após esta migração, então
-- order_ficha_ingredients nasce vazia e o custo de todos os pedidos existentes permanece
-- idêntico. Não há backfill: mover a sacola da ficha do produto para a ficha do pedido é
-- decisão do lojista.

-- Configuração viva, editável pelo lojista em "Configurar pedidos".
create table order_ficha_lines (
    id            uuid not null,
    merchant_id   uuid not null,
    ingredient_id uuid not null,
    quantity      numeric(19,6) not null,
    sort_order    integer,
    primary key (id)
);

alter table if exists order_ficha_lines
    add constraint FK_order_ficha_lines_merchant foreign key (merchant_id) references merchants;
alter table if exists order_ficha_lines
    add constraint FK_order_ficha_lines_ingredient foreign key (ingredient_id) references ingredients;
-- Uma linha por ingrediente: quantidade se soma numa linha só, não se repete a linha.
alter table if exists order_ficha_lines
    add constraint uk_order_ficha_lines_merchant_ingredient unique (merchant_id, ingredient_id);

create index idx_order_ficha_lines_merchant on order_ficha_lines (merchant_id);

-- Snapshot gravado em cada pedido no momento da criação/importação. Espelha
-- order_item_extra_ingredients: copia nome/unidade/custo por valor para que editar a ficha
-- ou o custo do ingrediente não reescreva retroativamente o custo de pedidos já fechados.
create table order_ficha_ingredients (
    id              uuid not null,
    order_id        uuid not null,
    ingredient_id   uuid not null,
    quantity        numeric(19,6) not null,
    cost_per_unit   numeric(19,4) not null,
    ingredient_name varchar(255) not null,
    ingredient_unit varchar(255) not null,
    primary key (id)
);

alter table if exists order_ficha_ingredients
    add constraint FK_order_ficha_ingredients_order foreign key (order_id) references orders;
alter table if exists order_ficha_ingredients
    add constraint FK_order_ficha_ingredients_ingredient foreign key (ingredient_id) references ingredients;

create index idx_order_ficha_ingredients_order on order_ficha_ingredients (order_id);
