-- Dashboard accounts. super_admin_guard is 1 for the super admin and NULL for everyone else;
-- the UNIQUE index on it means the DB itself allows at most ONE super admin.
CREATE TABLE admins (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    email             VARCHAR(150) NOT NULL,
    password_hash     VARCHAR(100) NOT NULL,
    name              VARCHAR(100) NOT NULL,
    role              VARCHAR(20)  NOT NULL,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by        BIGINT       NULL,
    last_login_at     DATETIME(6)  NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    super_admin_guard TINYINT GENERATED ALWAYS AS (CASE WHEN role = 'super_admin' THEN 1 ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_admins_email UNIQUE (email),
    CONSTRAINT uk_admins_single_super_admin UNIQUE (super_admin_guard),
    CONSTRAINT fk_admins_created_by FOREIGN KEY (created_by) REFERENCES admins (id),
    CONSTRAINT ck_admins_role CHECK (role IN ('admin', 'super_admin'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
