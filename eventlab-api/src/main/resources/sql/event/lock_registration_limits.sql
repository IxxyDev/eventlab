SELECT capacity, starts_at
FROM events
WHERE id = :id
FOR UPDATE;
