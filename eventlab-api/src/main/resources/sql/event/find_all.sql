SELECT
  events.id,
  events.title,
  events.description,
  events.starts_at,
  events.ends_at,
  events.time_zone,
  events.venue,
  events.capacity,
  organizations.name AS organization_name,
  (
    SELECT COUNT(*)
    FROM registrations
    WHERE registrations.event_id = events.id
  ) AS registration_count
FROM events
JOIN organizations ON organizations.id = events.organization_id
ORDER BY events.starts_at, events.id;
