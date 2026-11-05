-- A user's gender (MALE or FEMALE). Required when signing up or creating staff; existing accounts keep NULL
-- until they set it on their profile.
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender character varying(10);
ALTER TABLE users ADD CONSTRAINT users_gender_check CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE'));
