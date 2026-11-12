-- Courier-only data. id_front_url / id_back_url are storage keys of AES-GCM ENCRYPTED files.
CREATE TABLE courier_profiles (
    user_id           BIGINT       NOT NULL,
    address           VARCHAR(255) NOT NULL,
    vehicle_type      VARCHAR(20)  NOT NULL,
    vehicle_color     VARCHAR(30)  NOT NULL,
    plate_number      VARCHAR(20)  NULL,
    vehicle_photo_url VARCHAR(255) NOT NULL,
    id_front_url      VARCHAR(255) NOT NULL,
    id_back_url       VARCHAR(255) NOT NULL,
    approval_status   VARCHAR(20)  NOT NULL DEFAULT 'pending',
    review_note       VARCHAR(500) NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_courier_profiles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_courier_vehicle_type CHECK (vehicle_type IN ('car', 'scooter', 'motorcycle', 'bicycle')),
    CONSTRAINT ck_courier_approval CHECK (approval_status IN ('pending', 'needs_fix', 'approved', 'rejected'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
