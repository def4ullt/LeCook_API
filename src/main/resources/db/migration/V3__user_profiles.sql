CREATE TABLE user_books (
    user_id VARCHAR(128) PRIMARY KEY,
    revision BIGINT NOT NULL DEFAULT 0,
    data TEXT NOT NULL DEFAULT '{}',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE recipes ADD COLUMN protein DOUBLE PRECISION;
ALTER TABLE recipes ADD COLUMN fat DOUBLE PRECISION;
ALTER TABLE recipes ADD COLUMN carbs DOUBLE PRECISION;
CREATE TABLE user_publications (
    user_id VARCHAR(128) NOT NULL,
    local_id UUID NOT NULL,
    recipe_id UUID NOT NULL REFERENCES recipes(id),
    PRIMARY KEY(user_id,local_id)
);
CREATE TABLE profile_photos (
    user_id VARCHAR(128) NOT NULL,
    hash CHAR(64) NOT NULL,
    bytes BYTEA NOT NULL CHECK(octet_length(bytes)<=1048576),
    PRIMARY KEY(user_id,hash)
);
