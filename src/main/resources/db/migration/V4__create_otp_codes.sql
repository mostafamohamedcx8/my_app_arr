-- One row per OTP sent (WhatsApp). Only HMAC hashes are stored, never raw codes or tokens.
CREATE TABLE otp_codes (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    phone                   VARCHAR(20) NOT NULL,
    code_hash               CHAR(64)    NULL,
    purpose                 VARCHAR(20) NOT NULL,
    subject_type            VARCHAR(20) NOT NULL,
    expires_at              DATETIME(6) NOT NULL,
    attempts                INT         NOT NULL DEFAULT 0,
    request_ip              VARCHAR(45) NULL,
    verified_at             DATETIME(6) NULL,
    verification_token_hash CHAR(64)    NULL,
    verification_expires_at DATETIME(6) NULL,
    consumed_at             DATETIME(6) NULL,
    created_at              DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_otp_verification_token UNIQUE (verification_token_hash),
    INDEX ix_otp_phone_purpose (phone, purpose, id),
    INDEX ix_otp_phone_created (phone, created_at),
    INDEX ix_otp_ip_created (request_ip, created_at),
    CONSTRAINT ck_otp_purpose CHECK (purpose IN ('signup', 'reset_password')),
    CONSTRAINT ck_otp_subject CHECK (subject_type IN ('customer', 'courier', 'admin'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
