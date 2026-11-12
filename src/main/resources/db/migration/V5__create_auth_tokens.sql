-- Hash (HMAC-SHA256) of every issued JWT. A JWT is accepted only while its row is not revoked/expired.
CREATE TABLE auth_tokens (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    token_hash     CHAR(64)    NOT NULL,
    principal_type VARCHAR(10) NOT NULL,
    principal_id   BIGINT      NOT NULL,
    expires_at     DATETIME(6) NOT NULL,
    revoked_at     DATETIME(6) NULL,
    created_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_auth_tokens_hash UNIQUE (token_hash),
    INDEX ix_auth_tokens_principal (principal_type, principal_id),
    CONSTRAINT ck_auth_tokens_type CHECK (principal_type IN ('user', 'admin'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
