SELECT EXISTS (
  SELECT 1
  FROM registrations
  WHERE event_id = :eventId
    AND LOWER(email) = LOWER(:email)
);
