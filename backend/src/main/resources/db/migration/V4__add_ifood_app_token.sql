create table ifood_app_token (
    id                 uuid      primary key default gen_random_uuid(),
    access_token       text      not null,
    refresh_token      text      not null,
    expires_at         timestamp not null,
    refresh_expires_at timestamp not null,
    updated_at         timestamp not null default now()
);

alter table merchants
    add column ifood_merchant_id   text,
    add column ifood_authorized_at timestamp;
