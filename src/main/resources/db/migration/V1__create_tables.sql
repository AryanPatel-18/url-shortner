CREATE TABLE users (
   id UUID PRIMARY KEY,
   email VARCHAR(255) NOT NULL UNIQUE,
   password_hash VARCHAR(255) NOT NULL,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
   total_urls_created INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE shortened_urls (
    id UUID PRIMARY KEY,
    short_code VARCHAR(255) NOT NULL UNIQUE,
    original_url VARCHAR(2048) NOT NULL,
    status VARCHAR(255) NOT NULL,
    click_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id UUID NOT NULL,

    CONSTRAINT fk_shortened_urls_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
);