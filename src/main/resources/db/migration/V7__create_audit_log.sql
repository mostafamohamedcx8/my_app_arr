-- Append-only record of admin actions (approvals, money, bans, settings, catalog changes).
CREATE TABLE audit_log (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    admin_id    BIGINT      NOT NULL,
    action      VARCHAR(60) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id   BIGINT      NULL,
    details     TEXT        NULL,
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_log_admin FOREIGN KEY (admin_id) REFERENCES admins (id),
    INDEX ix_audit_log_target (target_type, target_id, created_at),
    INDEX ix_audit_log_admin (admin_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
