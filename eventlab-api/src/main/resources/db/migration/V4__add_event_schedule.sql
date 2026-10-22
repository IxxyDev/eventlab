ALTER TABLE events
  ADD COLUMN starts_at timestamptz,
  ADD COLUMN ends_at timestamptz,
  ADD COLUMN time_zone text;

UPDATE events
SET
  starts_at = '2026-11-14 18:00 Europe/Moscow',
  ends_at = '2026-11-14 20:00 Europe/Moscow',
  time_zone = 'Europe/Moscow'
WHERE id = 'sound-workshop';

ALTER TABLE events
  ALTER COLUMN starts_at SET NOT NULL,
  ALTER COLUMN ends_at SET NOT NULL,
  ALTER COLUMN time_zone SET NOT NULL,
  ADD CONSTRAINT events_schedule_order_check CHECK (ends_at > starts_at);
