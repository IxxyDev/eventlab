UPDATE registrations
SET access_token_hash = :accessTokenHash
WHERE id = :id;
