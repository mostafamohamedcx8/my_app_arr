-- Who reviewed a courier's application and when (the full history lives in audit_log).
ALTER TABLE courier_profiles
    ADD COLUMN reviewed_by BIGINT      NULL AFTER review_note,
    ADD COLUMN reviewed_at DATETIME(6) NULL AFTER reviewed_by,
    ADD CONSTRAINT fk_courier_profiles_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES admins (id),
    ADD INDEX ix_courier_profiles_approval (approval_status, created_at);
