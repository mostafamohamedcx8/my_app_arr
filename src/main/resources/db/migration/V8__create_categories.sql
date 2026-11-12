-- Customer home-screen categories (fish, grills, ...). Icon = a predefined key OR an uploaded image.
CREATE TABLE categories (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(60)  NOT NULL,
    icon_key   VARCHAR(60)  NULL,
    icon_url   VARCHAR(255) NULL,
    color      CHAR(7)      NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by BIGINT       NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT fk_categories_created_by FOREIGN KEY (created_by) REFERENCES admins (id),
    CONSTRAINT ck_categories_icon CHECK (icon_key IS NOT NULL OR icon_url IS NOT NULL),
    INDEX ix_categories_active_sort (is_active, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
