-- OrderStatus gained TEST (iFood test orders are now imported instead of
-- skipped) and NotificationType gained MISSING_PRODUCT and ORDER_CANCELLED.
-- The inline enum check constraints exported into V1 must be widened to
-- accept the new values, otherwise inserts fail at runtime.
alter table orders drop constraint if exists orders_status_check;
alter table orders add constraint orders_status_check
    check (status in ('PENDING','READY','DELIVERED','PAID','CANCELLED','TEST'));

alter table notifications drop constraint if exists notifications_type_check;
alter table notifications add constraint notifications_type_check
    check (type in ('MISSING_INGREDIENT','MISSING_PRODUCT','ORDER_CANCELLED'));
