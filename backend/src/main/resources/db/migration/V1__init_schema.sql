-- ============================================================================
-- V1 — Initial MenuBank schema
-- Generated from the JPA entity mappings (Hibernate DDL) so that the prod
-- profile (spring.jpa.hibernate.ddl-auto=validate) validates cleanly.
-- ============================================================================

create table merchants (
    created_at timestamp(6) not null,
    cnpj varchar(14) not null unique,
    id uuid not null,
    address varchar(500),
    logo_url varchar(500),
    anota_ai_api_key TEXT,
    email varchar(255) not null unique,
    merchant_name varchar(255) not null,
    password varchar(255),
    phone varchar(255),
    status varchar(255) not null check ((status in ('ACTIVE','INACTIVE'))),
    opening_hours jsonb,
    preferences jsonb,
    primary key (id)
);

create table identities (
    created_at timestamp(6) not null,
    id uuid not null,
    merchant_id uuid not null,
    provider varchar(255) not null,
    provider_user_id varchar(255) not null,
    primary key (id),
    unique (provider, provider_user_id)
);

create table categories (
    color_hex varchar(7),
    id uuid not null,
    merchant_id uuid not null,
    external_id varchar(255),
    name varchar(255) not null,
    primary key (id)
);

create table customers (
    id uuid not null,
    merchant_id uuid not null,
    neighborhood varchar(120),
    email varchar(255),
    external_id varchar(255),
    name varchar(255) not null,
    notes text,
    phone varchar(255),
    primary key (id)
);

create table fees (
    fee_rate numeric(5,4) not null,
    id uuid not null,
    merchant_id uuid not null,
    name varchar(255) not null,
    primary key (id)
);

create table ingredients (
    cost_per_unit numeric(19,4) not null,
    default_quantity numeric(19,4),
    low_stock_threshold numeric(19,4),
    sale_price numeric(19,4),
    stock_quantity numeric(19,4),
    last_replenished_at timestamp(6),
    id uuid not null,
    merchant_id uuid not null,
    canonical_name varchar(255),
    name varchar(255) not null,
    status varchar(255) not null check ((status in ('ACTIVE','INACTIVE'))),
    unit varchar(255) not null,
    primary key (id)
);

create table products (
    price numeric(38,2) not null,
    category_id uuid,
    id uuid not null,
    merchant_id uuid not null,
    external_id varchar(255),
    name varchar(255) not null,
    status varchar(255) not null check ((status in ('ACTIVE','INACTIVE'))),
    primary key (id)
);

create table includes (
    cost numeric(19,4) not null,
    quantity numeric(19,4) not null,
    sort_order integer,
    id uuid not null,
    product_id uuid not null,
    kind varchar(255) check ((kind in ('INGREDIENT','PACKAGING'))),
    name varchar(255) not null,
    primary key (id)
);

create table notifications (
    created_at timestamp(6) with time zone not null,
    resolved_at timestamp(6) with time zone,
    id uuid not null,
    merchant_id uuid not null,
    status varchar(32) not null check ((status in ('UNREAD','READ','RESOLVED'))),
    type varchar(64) not null check ((type in ('MISSING_INGREDIENT'))),
    message varchar(1000) not null,
    reference_data varchar(255),
    reference_display varchar(255),
    title varchar(255) not null,
    primary key (id)
);

create table orders (
    delivery_fee numeric(19,4),
    estimated_profit numeric(38,2) not null,
    total_cost numeric(19,4),
    total_value numeric(38,2) not null,
    date_time timestamp(6) not null,
    customer_id uuid not null,
    fee_id uuid,
    id uuid not null,
    merchant_id uuid not null,
    external_order_id varchar(255),
    origin varchar(255) check ((origin in ('MENUBANK','ANOTA_AI','IFOOD'))),
    status varchar(255) not null check ((status in ('PENDING','READY','DELIVERED','PAID','CANCELLED'))),
    primary key (id)
);

create table order_items (
    quantity integer not null,
    unit_cost numeric(19,4),
    unit_price numeric(38,2) not null,
    id uuid not null,
    order_id uuid not null,
    product_id uuid not null,
    primary key (id)
);

create table order_item_extra_ingredients (
    cost_per_unit numeric(19,4) not null,
    quantity numeric(19,6) not null,
    id uuid not null,
    ingredient_id uuid not null,
    order_item_id uuid not null,
    ingredient_name varchar(255) not null,
    ingredient_unit varchar(255) not null,
    primary key (id)
);

-- Foreign keys
alter table if exists categories add constraint FKp88pi8dlymagblpfl01nnoly9 foreign key (merchant_id) references merchants;
alter table if exists customers add constraint FK78bsbhxdqsk8w9csgikhnlhx2 foreign key (merchant_id) references merchants;
alter table if exists fees add constraint FKrfscsb3liinyewrkp1i74hg3l foreign key (merchant_id) references merchants;
alter table if exists includes add constraint FKj39eaenbrdjcr7uw526vwg3ed foreign key (product_id) references products;
alter table if exists ingredients add constraint FKs4w48e83gtnintqegm8w52n44 foreign key (merchant_id) references merchants;
alter table if exists notifications add constraint FK7jyhrsvla4aaj01ksj8v3sub3 foreign key (merchant_id) references merchants;
alter table if exists order_item_extra_ingredients add constraint FK59py0a3qyn8lux1ngwga6ehu8 foreign key (ingredient_id) references ingredients;
alter table if exists order_item_extra_ingredients add constraint FKbpkrt5pynay90pa6vdhfskf0s foreign key (order_item_id) references order_items;
alter table if exists order_items add constraint FKbioxgbv59vetrxe0ejfubep1w foreign key (order_id) references orders;
alter table if exists order_items add constraint FKocimc7dtr037rh4ls4l95nlfi foreign key (product_id) references products;
alter table if exists orders add constraint FKpxtb8awmi0dk6smoh2vp1litg foreign key (customer_id) references customers;
alter table if exists orders add constraint FK37klvjj89xih62qth9g9t1ipf foreign key (fee_id) references fees;
alter table if exists orders add constraint FKtqvpn5gy0ajx8fip3deoidfxd foreign key (merchant_id) references merchants;
alter table if exists products add constraint FKog2rp4qthbtt2lfyhfo32lsw9 foreign key (category_id) references categories;
alter table if exists products add constraint FKt1yvv81v320ba41fq28k7had2 foreign key (merchant_id) references merchants;
