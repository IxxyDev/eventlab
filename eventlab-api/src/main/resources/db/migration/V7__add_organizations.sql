CREATE TABLE organizations (
  id text PRIMARY KEY,
  name text NOT NULL
);

INSERT INTO organizations (id, name)
VALUES ('mixer-school', 'Школа звукорежиссуры «Микшер»');

ALTER TABLE events
  ADD COLUMN organization_id text REFERENCES organizations (id);

UPDATE events
SET organization_id = 'mixer-school'
WHERE id = 'sound-workshop';

ALTER TABLE events
  ALTER COLUMN organization_id SET NOT NULL;
