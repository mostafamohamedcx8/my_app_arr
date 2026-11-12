-- Wallet the customer pays into (filled by the courier on first login after approval)
-- and tracking of re-submitted applications after a needs_fix decision.
ALTER TABLE courier_profiles
    ADD COLUMN wallet_number      VARCHAR(100) NULL AFTER address,
    ADD COLUMN wallet_type        VARCHAR(20)  NULL AFTER wallet_number,
    ADD COLUMN resubmitted_at     DATETIME(6)  NULL AFTER reviewed_at,
    ADD COLUMN resubmission_count INT          NOT NULL DEFAULT 0 AFTER resubmitted_at,
    ADD CONSTRAINT ck_courier_wallet_type CHECK (wallet_type IS NULL OR wallet_type IN ('vodafone_cash', 'instapay'));
