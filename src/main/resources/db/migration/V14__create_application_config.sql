-- All business values (fees, commission, limits, points, service area) live here — never hardcoded.
-- `config_key` is the key (KEY is a reserved word in MySQL/MariaDB). Values are text, parsed by value_type.
-- An empty value means "not set yet" (e.g. the service area must be set by the super admin).
CREATE TABLE application_config (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    config_key  VARCHAR(100) NOT NULL,
    value       VARCHAR(255) NOT NULL DEFAULT '',
    value_type  VARCHAR(10)  NOT NULL,
    description VARCHAR(255) NULL,
    updated_by  BIGINT       NULL,
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_application_config_key UNIQUE (config_key),
    CONSTRAINT ck_application_config_type CHECK (value_type IN ('INT', 'DECIMAL', 'STRING', 'BOOLEAN')),
    CONSTRAINT fk_application_config_updated_by FOREIGN KEY (updated_by) REFERENCES admins (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO application_config (config_key, value, value_type, description) VALUES
    ('base_fee',                 '15',  'DECIMAL', 'Shops order: fee for the first shop (EGP)'),
    ('extra_shop_fee',           '5',   'DECIMAL', 'Shops order: fee for every extra shop (EGP)'),
    ('max_shops_per_order',      '5',   'INT',     'Shops order: maximum number of shops'),
    ('anything_min_fee',         '15',  'DECIMAL', 'Anything order: minimum fee (EGP)'),
    ('anything_no_pickup_max',   '30',  'DECIMAL', 'Anything order without pickup point: maximum fee (EGP)'),
    ('anything_base_max',        '25',  'DECIMAL', 'Anything order with pickup: maximum fee up to the free distance (EGP)'),
    ('anything_free_km',         '3.5', 'DECIMAL', 'Anything order with pickup: road km included in the base maximum'),
    ('anything_per_km_fee',      '7',   'DECIMAL', 'Anything order with pickup: added to the maximum per started km beyond the free km (EGP)'),
    ('commission_rate',          '20',  'DECIMAL', 'Commission % of the delivery fee, frozen on the order at acceptance'),
    ('points_per_egp',           '1',   'DECIMAL', 'Points on delivery = floor(delivery fee x this), for customer and courier'),
    ('order_offer_ttl_minutes',  '10',  'INT',     'Minutes a courier offer stays valid'),
    ('order_search_ttl_minutes', '30',  'INT',     'Minutes an order searches for a courier'),
    ('service_area_lat',         '',    'DECIMAL', 'Service area centre latitude — set by the super admin'),
    ('service_area_lng',         '',    'DECIMAL', 'Service area centre longitude — set by the super admin'),
    ('service_area_radius_km',   '',    'DECIMAL', 'Service area radius (km, straight line) — set by the super admin');
