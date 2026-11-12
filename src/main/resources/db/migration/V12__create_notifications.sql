-- In-app notifications for customers and couriers (also mirrored to WhatsApp where relevant).
CREATE TABLE notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    title      VARCHAR(150) NOT NULL,
    body       VARCHAR(1000) NOT NULL,
    link_type  VARCHAR(30)  NULL,
    link_id    BIGINT       NULL,
    read_at    DATETIME(6)  NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_notifications_user (user_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
