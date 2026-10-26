SELECT id
FROM events
WHERE id = :id
FOR UPDATE;
