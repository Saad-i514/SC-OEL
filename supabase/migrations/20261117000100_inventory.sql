create table public.inventory_items (
    id bigint generated always as identity primary key,
    name text not null check (length(btrim(name)) between 1 and 200),
    quantity integer not null check (quantity >= 0),
    status text not null check (status in ('In Stock', 'Low Stock', 'Out of Stock'))
);
alter table public.inventory_items enable row level security;
revoke all on public.inventory_items from anon, authenticated;
comment on table public.inventory_items is 'OEL inventory; trusted desktop JDBC access only';
