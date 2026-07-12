-- The free trial was removed: subscriptions are now created with status
-- PENDING at registration and only become ACTIVE after the first payment.
-- The inline enum check constraint from V2 must be widened to accept PENDING,
-- otherwise inserting a pending subscription fails at runtime. Existing TRIAL
-- rows are kept as-is.
alter table subscriptions drop constraint if exists subscriptions_status_check;
alter table subscriptions add constraint subscriptions_status_check
    check (status in ('PENDING','TRIAL','ACTIVE','PAST_DUE','CANCELED'));
