create table plans (
    id uuid not null primary key,
    name varchar(100) not null,
    min_revenue numeric(15,2) not null,
    max_revenue numeric(15,2),
    price_monthly numeric(10,2) not null,
    features jsonb not null default '{}',
    active boolean not null default true,
    created_at timestamp(6) not null,
    constraint plans_min_revenue_check check (min_revenue >= 0),
    constraint plans_price_check check (price_monthly >= 0),
    constraint plans_max_revenue_check check (max_revenue is null or max_revenue > min_revenue)
);

create table subscriptions (
    id uuid not null primary key,
    merchant_id uuid not null unique,
    plan_id uuid,
    status varchar(20) not null,
    trial_ends_at timestamp(6),
    current_period_start timestamp(6),
    current_period_end timestamp(6),
    stripe_subscription_id varchar(255) unique,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    constraint subscriptions_status_check check (status in ('TRIAL','ACTIVE','PAST_DUE','CANCELED')),
    constraint fk_subscription_merchant foreign key (merchant_id) references merchants(id),
    constraint fk_subscription_plan foreign key (plan_id) references plans(id)
);

create index idx_subscriptions_merchant on subscriptions(merchant_id);
create index idx_subscriptions_status on subscriptions(status);
create index idx_subscriptions_trial_ends_at on subscriptions(trial_ends_at);

create table revenue_reports (
    id uuid not null primary key,
    merchant_id uuid not null,
    reported_revenue numeric(15,2) not null,
    reference_month date not null,
    suggested_plan_id uuid,
    created_at timestamp(6) not null,
    constraint revenue_reports_revenue_check check (reported_revenue >= 0),
    constraint revenue_reports_unique unique (merchant_id, reference_month),
    constraint fk_revenue_report_merchant foreign key (merchant_id) references merchants(id),
    constraint fk_revenue_report_plan foreign key (suggested_plan_id) references plans(id)
);

create index idx_revenue_reports_merchant_month on revenue_reports(merchant_id, reference_month);

create table invoices (
    id uuid not null primary key,
    subscription_id uuid not null,
    amount numeric(10,2) not null,
    status varchar(20) not null,
    stripe_invoice_id varchar(255) unique,
    paid_at timestamp(6),
    due_at timestamp(6) not null,
    created_at timestamp(6) not null,
    constraint invoices_status_check check (status in ('PENDING','PAID','FAILED')),
    constraint invoices_amount_check check (amount > 0),
    constraint fk_invoice_subscription foreign key (subscription_id) references subscriptions(id)
);

create index idx_invoices_subscription on invoices(subscription_id);
create index idx_invoices_status on invoices(status);
create index idx_invoices_due_at on invoices(due_at);
