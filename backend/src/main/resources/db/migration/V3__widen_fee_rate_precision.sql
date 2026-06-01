-- feeRate stores a whole-number percentage (e.g. 2.5 = 2.5%), applied as
-- totalValue × feeRate / 100. numeric(5,4) only allowed values < 10, so any
-- rate of 10% or more (e.g. iFood 12%) failed to persist with a numeric field
-- overflow. Widen to numeric(7,4) to match @Digits(integer = 3, fraction = 4)
-- on FeeRequest, allowing rates up to 999.9999%.
alter table fees alter column fee_rate type numeric(7,4);
