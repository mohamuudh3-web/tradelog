-- TradeLog ↔ web sync schema for Supabase (Postgres).
-- Run this in Supabase → SQL Editor → New query → Run.
--
-- Design notes:
--  * Every row has a client-generated `uid` (text UUID) so the Android app and the
--    web app refer to the same record. Sync = "last write wins" via `updated_at`,
--    soft-delete via `deleted` so deletions propagate.
--  * `user_id` defaults to the logged-in user; Row-Level Security keeps data private.
--  * Screenshots upload to the `screenshots` storage bucket; rows store the public URL.

-- ---------- helper: standard columns macro (applied per table below) ----------
-- uid text unique, user_id uuid, updated_at bigint (epoch ms), deleted boolean

create extension if not exists "pgcrypto";

-- ===================== TABLES =====================

create table if not exists accounts (
  uid text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null default '',
  broker text not null default '',
  balance double precision not null default 0,
  currency text not null default 'USD',
  is_prop_firm boolean not null default false,
  challenge_phase text not null default '',
  status text not null default '',
  website text not null default '',
  starting_balance double precision,
  split_percent double precision,
  drawdown_percent double precision,
  target_percent double precision,
  created_at bigint not null default 0,
  updated_at bigint not null default 0,
  deleted boolean not null default false
);

create table if not exists trades (
  uid text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  account_uid text,
  instrument text not null default '',
  direction text not null default 'LONG',
  entry_price double precision not null default 0,
  exit_price double precision,
  lot_size double precision not null default 0,
  risk_percent double precision,
  r_multiple double precision,
  result text not null default 'BREAKEVEN',
  pnl double precision not null default 0,
  setup_tag text,
  session text not null default '',
  sl_pips double precision,
  tp_pips double precision,
  psychology text not null default '',
  checked_rules text not null default '',
  image_urls text not null default '',
  notes text not null default '',
  screenshot_url text,
  opened_at bigint not null default 0,
  created_at bigint not null default 0,
  updated_at bigint not null default 0,
  deleted boolean not null default false
);

create table if not exists journal_entries (
  uid text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  date text not null default '',
  mindset text not null default '',
  routine text not null default '',
  reflection text not null default '',
  mood int not null default 3,
  discipline int not null default 3,
  title text not null default '',
  gratitude text not null default '',
  battle_plan text not null default '',
  affirmation text not null default '',
  tags text not null default '',
  mood_label text not null default '',
  focus_tasks text not null default '',
  account_balance double precision,
  trades_target int,
  pips_target double precision,
  risk_percent double precision,
  risk_amount double precision,
  created_at bigint not null default 0,
  updated_at bigint not null default 0,
  deleted boolean not null default false
);

create table if not exists notes (
  uid text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  title text not null default '',
  body text not null default '',
  tags text not null default '',
  updated_at bigint not null default 0,
  deleted boolean not null default false
);

create table if not exists payouts (
  uid text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  date text not null default '',
  account_name text not null default '',
  amount double precision not null default 0,
  currency text not null default 'USD',
  status text not null default 'PENDING',
  notes text not null default '',
  created_at bigint not null default 0,
  updated_at bigint not null default 0,
  deleted boolean not null default false
);

create table if not exists goals (
  uid text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  title text not null default '',
  type text not null default 'DAILY',
  metric text not null default 'MANUAL',
  target int not null default 1,
  manual_progress int not null default 0,
  manual_period_key text not null default '',
  unit text not null default '',
  archived boolean not null default false,
  created_at bigint not null default 0,
  updated_at bigint not null default 0,
  deleted boolean not null default false
);

create table if not exists tasks (
  uid text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  title text not null default '',
  frequency text not null default 'DAILY',
  category text not null default 'TASK',
  last_completed_date text,
  done_once boolean not null default false,
  sort_order int not null default 0,
  created_at bigint not null default 0,
  updated_at bigint not null default 0,
  deleted boolean not null default false
);

create table if not exists instruments (
  uid text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null default '',
  pip_value_per_lot double precision not null default 10,
  sort_order int not null default 0,
  updated_at bigint not null default 0,
  deleted boolean not null default false
);

create table if not exists checklist_rules (
  uid text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  text text not null default '',
  sort_order int not null default 0,
  updated_at bigint not null default 0,
  deleted boolean not null default false
);

create table if not exists position_presets (
  uid text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null default '',
  balance double precision not null default 0,
  risk_percent double precision not null default 1,
  stop_loss double precision not null default 10,
  pip_value_per_lot double precision not null default 10,
  instrument text not null default '',
  updated_at bigint not null default 0,
  deleted boolean not null default false
);

create table if not exists backtests (
  uid text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  title text not null default '',
  instrument text not null default '',
  date_millis bigint not null default 0,
  bias text not null default '',
  direction text not null default '',
  result text not null default '',
  session text not null default '',
  sl_pips double precision,
  tp_pips double precision,
  checked_rules text not null default '',
  image_urls text not null default '',
  chart5_url text not null default '',
  chart15_url text not null default '',
  notes text not null default '',
  created_at bigint not null default 0,
  updated_at bigint not null default 0,
  deleted boolean not null default false
);

create table if not exists countdowns (
  uid text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  title text not null default '',
  target_date_millis bigint not null default 0,
  motivation text not null default '',
  reminder_hour int not null default 7,
  reminder_minute int not null default 0,
  review_done boolean not null default false,
  reached_it boolean not null default false,
  went_wrong text not null default '',
  improve_next text not null default '',
  created_at bigint not null default 0,
  updated_at bigint not null default 0,
  deleted boolean not null default false
);

-- ===================== ROW-LEVEL SECURITY =====================
-- Each user can only read/write their own rows.
do $$
declare t text;
begin
  foreach t in array array[
    'accounts','trades','journal_entries','notes','payouts','goals','tasks',
    'instruments','checklist_rules','position_presets','backtests','countdowns'
  ] loop
    execute format('alter table %I enable row level security;', t);
    execute format('drop policy if exists own_rows on %I;', t);
    execute format(
      'create policy own_rows on %I for all using (user_id = auth.uid()) with check (user_id = auth.uid());', t);
  end loop;
end $$;

-- ===================== STORAGE (screenshots) =====================
insert into storage.buckets (id, name, public)
values ('screenshots', 'screenshots', true)
on conflict (id) do nothing;

drop policy if exists "screenshots read" on storage.objects;
create policy "screenshots read" on storage.objects
  for select using (bucket_id = 'screenshots');

drop policy if exists "screenshots write own" on storage.objects;
create policy "screenshots write own" on storage.objects
  for insert with check (bucket_id = 'screenshots' and auth.role() = 'authenticated');

drop policy if exists "screenshots update own" on storage.objects;
create policy "screenshots update own" on storage.objects
  for update using (bucket_id = 'screenshots' and auth.role() = 'authenticated');

drop policy if exists "screenshots delete own" on storage.objects;
create policy "screenshots delete own" on storage.objects
  for delete using (bucket_id = 'screenshots' and auth.role() = 'authenticated');
