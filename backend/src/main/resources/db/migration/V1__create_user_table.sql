CREATE TABLE app_user (
                     id          BIGSERIAL PRIMARY KEY,
                     email       VARCHAR(255) NOT NULL UNIQUE,
                     password_hash VARCHAR(255) NOT NULL,
                     full_name   VARCHAR(255),
                     created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                     updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE TABLE desk (
                     id          BIGSERIAL PRIMARY KEY,
                     name        VARCHAR(255) NOT NULL,
                     location    VARCHAR(255)
);