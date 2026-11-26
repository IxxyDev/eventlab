TRUNCATE registrations;

UPDATE events
SET
  starts_at = now() + interval '30 days',
  ends_at = now() + interval '30 days 2 hours'
WHERE id IN ('sound-workshop', 'stage-lighting');
