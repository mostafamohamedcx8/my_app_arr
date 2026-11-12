-- needs_fix: which fields the admin asked the courier to correct (comma-separated API field keys).
ALTER TABLE courier_profiles
    ADD COLUMN fix_fields VARCHAR(500) NULL AFTER review_note;

-- New OTP purpose: a needs_fix courier opens the application by phone number + WhatsApp code.
ALTER TABLE otp_codes DROP CONSTRAINT ck_otp_purpose;
ALTER TABLE otp_codes ADD CONSTRAINT ck_otp_purpose CHECK (purpose IN ('signup', 'reset_password', 'courier_fix'));
