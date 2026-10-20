CREATE TABLE events (
  id text PRIMARY KEY,
  title text NOT NULL,
  description text NOT NULL,
  date_label text NOT NULL,
  time_label text NOT NULL,
  venue text NOT NULL,
  capacity integer NOT NULL CHECK (capacity > 0)
);

CREATE TABLE registrations (
  id uuid PRIMARY KEY,
  event_id text NOT NULL REFERENCES events (id),
  name text NOT NULL CHECK (CHAR_LENGTH(name) BETWEEN 1 AND 100),
  email text NOT NULL CHECK (CHAR_LENGTH(email) BETWEEN 1 AND 254),
  access_token text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX registrations_event_email_key
  ON registrations (event_id, LOWER(email));
