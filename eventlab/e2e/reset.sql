TRUNCATE registrations;

UPDATE events
SET
  starts_at = now() + interval '30 days',
  ends_at = now() + interval '30 days 2 hours';
