-- Shops inside a category. opens_at/closes_at are Africa/Cairo wall-clock times;
-- closes_at < opens_at means the shop closes after midnight, equal means open 24h.
CREATE TABLE shops (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    category_id BIGINT        NOT NULL,
    name        VARCHAR(120)  NOT NULL,
    photo_url   VARCHAR(255)  NOT NULL,
    street      VARCHAR(255)  NOT NULL,
    lat         DECIMAL(10,7) NOT NULL,
    lng         DECIMAL(10,7) NOT NULL,
    opens_at    TIME          NOT NULL,
    closes_at   TIME          NOT NULL,
    is_open     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by  BIGINT        NULL,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_shops_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_shops_created_by FOREIGN KEY (created_by) REFERENCES admins (id),
    INDEX ix_shops_category_name (category_id, name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
