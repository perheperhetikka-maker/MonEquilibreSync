-- Additive schema for Mon équilibre Sync.
-- Already applied to the connected production database on 2026-09-23.

create table if not exists public.health_connect_daily (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  entry_date date not null,
  steps integer not null check (steps >= 0 and steps <= 200000),
  source text not null default 'health_connect',
  synced_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  unique (user_id, entry_date)
);

create table if not exists public.health_connect_exercises (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  health_connect_record_id text not null,
  source_package text not null,
  exercise_type integer not null,
  exercise_label text not null,
  start_time timestamptz not null,
  end_time timestamptz not null,
  duration_minutes integer not null check (duration_minutes >= 0 and duration_minutes <= 10080),
  distance_meters numeric not null default 0 check (distance_meters >= 0),
  synced_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  unique (user_id, health_connect_record_id)
);

alter table public.health_connect_daily enable row level security;
alter table public.health_connect_exercises enable row level security;

create policy "Users manage their own Health Connect daily data"
on public.health_connect_daily for all to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "Users manage their own Health Connect exercises"
on public.health_connect_exercises for all to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

revoke all on public.health_connect_daily from anon;
revoke all on public.health_connect_exercises from anon;
grant select, insert, update, delete on public.health_connect_daily to authenticated;
grant select, insert, update, delete on public.health_connect_exercises to authenticated;
