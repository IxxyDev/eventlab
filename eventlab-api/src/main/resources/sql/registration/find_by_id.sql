SELECT id, event_id, name, email, acces_token_hash, idempotency_key_hash
FROM registrations
WHERE id = :id;
