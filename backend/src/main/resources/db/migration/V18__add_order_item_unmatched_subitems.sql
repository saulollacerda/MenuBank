-- SubItems (Anota.AI) / options (iFood) de pedidos importados que NÃO casaram com nenhum
-- ingrediente cadastrado. Antes eram apenas notificados e descartados; agora são gravados
-- por item de pedido para aparecerem no detalhe do pedido com um botão de "cadastrar
-- ingrediente". Depois que o ingrediente é criado, a resposta filtra o registro pelo nome
-- canônico e o botão some — o registro persiste, mas não é mais exibido.
--
-- Retrocompatível: pedidos importados antes desta migração não têm registros, então
-- order_item_unmatched_subitems nasce vazia e nada muda para eles.
create table order_item_unmatched_subitems (
    id                  uuid not null,
    order_item_id       uuid not null,
    raw_name            varchar(255) not null,
    canonical_name      varchar(255) not null,
    quantity            integer not null,
    sale_price_per_unit numeric(19,4),
    sale_price_total    numeric(19,4),
    primary key (id)
);

alter table if exists order_item_unmatched_subitems
    add constraint FK_order_item_unmatched_subitems_order_item
    foreign key (order_item_id) references order_items;

create index idx_order_item_unmatched_subitems_item
    on order_item_unmatched_subitems (order_item_id);
