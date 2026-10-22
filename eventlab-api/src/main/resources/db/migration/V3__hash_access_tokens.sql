ALTER TABLE registrations
  ADD COLUMN access_token_hash bytea
    CHECK (OCTET_LENGTH(access_token_hash) = 32);

UPDATE registrations
SET access_token_hash = SHA256(CONVERT_TO(access_token, 'UTF8'));

ALTER TABLE registrations
  ALTER COLUMN access_token_hash SET NOT NULL,
  DROP COLUMN access_token;
