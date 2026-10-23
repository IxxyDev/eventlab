SELECT id, event_id, name, email, access_token_hash, idempotency_key_hash
FROM registrations
WHERE idempotency_key_hash = :idempotencyKeyHash;
