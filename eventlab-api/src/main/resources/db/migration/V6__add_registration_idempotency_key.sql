ALTER TABLE registrations
  ADD COLUMN idempotency_key_hash bytea
    CHECK (OCTET_LENGTH(idempotency_key_hash) = 32);

CREATE UNIQUE INDEX registrations_idempotency_key_hash_key
  ON registrations (idempotency_key_hash);
