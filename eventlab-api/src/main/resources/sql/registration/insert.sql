INSERT INTO registrations (
  id, event_id, name, email, access_token_hash, idempotency_key_hash
)
VALUES (
  :id, :eventId, :name, :email, :accessTokenHash, :idempotencyKeyHash
)
ON CONFLICT (idempotency_key_hash) DO NOTHING;
