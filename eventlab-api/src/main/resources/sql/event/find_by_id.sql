SELECT
  id,
  title,
  description,
  starts_at,
  ends_at,
  time_zone,
  venue,
  capacity,
  (
    SELECT COUNT(*)
    FROM registrations
    WHERE registrations.event_id = events.id
  ) AS registration_count
FROM events
WHERE id = :id;
