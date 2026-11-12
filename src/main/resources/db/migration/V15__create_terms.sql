-- Terms & conditions, versioned per audience. Exactly one current version per audience (current_guard).
-- content format: plain Arabic text; a line starting with "## " is a section title, the lines after it its body.
CREATE TABLE terms_versions (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    audience      VARCHAR(10) NOT NULL,
    version       INT         NOT NULL,
    content       TEXT        NOT NULL,
    published_at  DATETIME(6) NOT NULL,
    is_current    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_by    BIGINT      NULL,
    current_guard VARCHAR(10) GENERATED ALWAYS AS (CASE WHEN is_current THEN audience ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_terms_versions_audience_version UNIQUE (audience, version),
    CONSTRAINT uk_terms_versions_one_current UNIQUE (current_guard),
    CONSTRAINT ck_terms_versions_audience CHECK (audience IN ('CUSTOMER', 'COURIER')),
    CONSTRAINT ck_terms_versions_version CHECK (version >= 1),
    CONSTRAINT fk_terms_versions_created_by FOREIGN KEY (created_by) REFERENCES admins (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Legal record: append-only. UPDATE / DELETE are blocked by the triggers below.
CREATE TABLE terms_acceptances (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    terms_version_id BIGINT       NOT NULL,
    accepted_at      DATETIME(6)  NOT NULL,
    ip_address       VARCHAR(45)  NULL,
    user_agent       VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_terms_acceptances_user_version UNIQUE (user_id, terms_version_id),
    CONSTRAINT fk_terms_acceptances_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_terms_acceptances_version FOREIGN KEY (terms_version_id) REFERENCES terms_versions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TRIGGER trg_terms_acceptances_no_update BEFORE UPDATE ON terms_acceptances
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'terms_acceptances is append-only';

CREATE TRIGGER trg_terms_acceptances_no_delete BEFORE DELETE ON terms_acceptances
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'terms_acceptances is append-only';

-- Signup step 1 records which version the app showed; step 3 turns it into a terms_acceptances row.
ALTER TABLE otp_codes
    ADD COLUMN terms_version_id BIGINT NULL AFTER request_ip,
    ADD COLUMN user_agent VARCHAR(255) NULL AFTER terms_version_id,
    ADD CONSTRAINT fk_otp_codes_terms_version FOREIGN KEY (terms_version_id) REFERENCES terms_versions (id);

-- Version 1 = the text the apps showed until now (draft — must be reviewed by a lawyer before launch).
INSERT INTO terms_versions (audience, version, content, published_at, is_current) VALUES
('CUSTOMER', 1, '## ١. عن الخدمة
اقضيها منصة بتوصّلك بمندوب بيشتري طلباتك من المحلات ويوصّلها لحد عندك. التطبيق مش بائع للمنتجات، والمحل هو المسؤول عن جودتها.

## ٢. الطلبات والأسعار
سعر التوصيل بيظهرلك قبل ما توافق. المندوب بيعمل فاتورة لكل محل فيها الحاجات وأسعارها ومعاها صورة، وبتوصلك أول بأول، وفي الآخر فاتورة نهائية بكل المحلات والتوصيل.

## ٣. الإلغاء
تقدر تلغي الأوردر لحد ما المندوب يعمل أول فاتورة. المندوب يقدر يلغي لحد ما يستلم الحاجة من المحل، وبعد الاستلام مينفعش إلغاء من الطرفين. رفض استلام الأوردر ممكن يوقف حسابك.

## ٤. الدفع
الدفع بيكون بينك وبين المندوب مباشرة: كاش، أو تحويل على رقم محفظته اللي ظاهر في الأوردر. التطبيق مش بياخد منك أي فلوس.

## ٥. حسابك
إنت مسؤول عن صحة بياناتك ورقمك. من حقنا نوقف أي حساب بيسيء استخدام الخدمة بشكل مؤقت أو دائم.

## ٦. الخصوصية
بنستخدم رقمك وعنوانك عشان نوصّلك طلبك ونتواصل معاك بس، ومش بنبيع بياناتك لأي حد.', UTC_TIMESTAMP(6), TRUE),
('COURIER', 1, '## ١. عن الخدمة
اقضيها منصة بتوصّل العملاء بمناديب من المنطقة بيشتروا الطلبات من المحلات ويوصّلوها. إنت بتشتغل لحسابك، والتطبيق بيوصّلك بالطلبات بس.

## ٢. الطلبات والفواتير
لكل محل لازم تعمل فاتورة فيها الحاجات وأسعارها ومعاها صورة من كاميرا التطبيق، والعميل بيشوفها أول بأول.

## ٣. الإلغاء
تقدر تلغي الأوردر لحد ما تستلم الحاجة من المحل، وبعد الاستلام مينفعش إلغاء.

## ٤. الدفع
العميل بيدفعلك مباشرة: كاش، أو تحويل على رقم محفظتك. التطبيق مش بيستلم فلوس من العملاء.

## ٥. العمولة والإيقاف
التطبيق بياخد عمولة من سعر التوصيل بس، والنسبة بتظهرلك قبل ما تقبل الأوردر. العمولة بتتجمع عليك، ولما توصل للحد المسموح الحساب بيقف لحد ما تسدد. أول ما ترفع إثبات السداد بيتفتحلك سلفة لحد ما الإدارة تأكد.

## ٦. حسابك
إنت مسؤول عن صحة بياناتك وصور البطاقة والعربية. من حقنا نوقف أي حساب بيسيء استخدام الخدمة بشكل مؤقت أو دائم.

## ٧. الخصوصية
صور البطاقة متشفرة ومحدش بيشوفها غير الإدارة. بنستخدم بياناتك عشان تشغيل الخدمة بس، ومش بنبيعها لأي حد.', UTC_TIMESTAMP(6), TRUE);

-- Accounts created before versioning accepted the in-app terms at signup → record that as version 1.
INSERT INTO terms_acceptances (user_id, terms_version_id, accepted_at, ip_address, user_agent)
SELECT u.id, tv.id, u.terms_accepted_at, NULL, 'backfill: accepted at signup before terms versioning'
FROM users u
JOIN terms_versions tv ON tv.version = 1 AND tv.audience = UPPER(u.role);
