-- Insumos da ficha técnica desmarcados por item de pedido manual.
-- Vazio = ficha completa entra no custo do item.
CREATE TABLE order_item_excluded_includes (
    order_item_id UUID NOT NULL REFERENCES order_items (id) ON DELETE CASCADE,
    include_id    UUID NOT NULL,
    PRIMARY KEY (order_item_id, include_id)
);
