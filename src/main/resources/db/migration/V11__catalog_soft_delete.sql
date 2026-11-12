-- Categories and shops are soft-deleted (deleted_at) so future orders keep valid references.
-- Category names stay unique among NON-deleted rows only: name_guard is NULL once deleted.
ALTER TABLE categories
    DROP INDEX uk_categories_name,
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN name_guard VARCHAR(60) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN name ELSE NULL END) STORED,
    ADD CONSTRAINT uk_categories_name_alive UNIQUE (name_guard);

ALTER TABLE shops
    ADD COLUMN deleted_at DATETIME(6) NULL;
