DROP TABLE IF EXISTS shortened_urls CASCADE;

CREATE TABLE shortened_urls (
    id UUID PRIMARY KEY,
    original_url TEXT NOT NULL UNIQUE,
    short_code VARCHAR(20) NOT NULL UNIQUE,
    click_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE user_urls (
   id UUID PRIMARY KEY,
   user_id UUID NOT NULL,
   url_id UUID NOT NULL,
   status VARCHAR(50) NOT NULL,
   created_at TIMESTAMP NOT NULL,
   updated_at TIMESTAMP NOT NULL,

   CONSTRAINT fk_user_urls_user
       FOREIGN KEY (user_id)
           REFERENCES users(id),

   CONSTRAINT fk_user_urls_shortened_url
       FOREIGN KEY (url_id)
           REFERENCES shortened_urls(id)
);