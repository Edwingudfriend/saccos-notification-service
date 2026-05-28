-- ============================================================
-- V1__notification_schema.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS notifications (
    id              VARCHAR(36)     NOT NULL DEFAULT (UUID()),
    member_id       VARCHAR(36)     NOT NULL,
    reference_id    VARCHAR(100),
    event           VARCHAR(50)     NOT NULL,
    channel         VARCHAR(20)     NOT NULL,
    recipient       VARCHAR(200)    NOT NULL,
    subject         VARCHAR(300),
    body            TEXT            NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    retry_count     INT             NOT NULL DEFAULT 0,
    error_message   TEXT,
    sent_at         DATETIME,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id       VARCHAR(50)     NOT NULL DEFAULT 'default',
    PRIMARY KEY (id),
    INDEX idx_notif_member  (member_id),
    INDEX idx_notif_ref     (reference_id),
    INDEX idx_notif_status  (status),
    INDEX idx_notif_tenant  (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification_templates (
    id          VARCHAR(36)     NOT NULL DEFAULT (UUID()),
    event       VARCHAR(50)     NOT NULL,
    channel     VARCHAR(20)     NOT NULL,
    language    VARCHAR(10)     NOT NULL DEFAULT 'sw',
    subject     VARCHAR(300),
    body        TEXT            NOT NULL,
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uq_template (event, channel, language)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Seed Swahili SMS templates ─────────────────────────────
INSERT IGNORE INTO notification_templates (id, event, channel, language, body) VALUES
(UUID(), 'MEMBER_CREATED',              'SMS', 'sw', 'Karibu SACCOS! Namba yako ya uanachama ni {{memberNumber}}. Tafadhali kamilisha uthibitisho wa KYC.'),
(UUID(), 'MEMBER_ACTIVATED',            'SMS', 'sw', 'Hongera {{name}}! Akaunti yako RA-SACCOS imethibitishwa. Namba: {{memberNumber}}.'),
(UUID(), 'LOAN_APPROVED',               'SMS', 'sw', 'Mkopo wako wa TZS {{amount}} umeidhinishwa. Utaingizwa akaunti yako hivi karibuni. Kumb: {{reference}}.'),
(UUID(), 'LOAN_REJECTED',               'SMS', 'sw', 'Ombi lako la mkopo (Kumb: {{reference}}) limekataliwa. Sababu: {{reason}}. Wasiliana na ofisi kwa maelezo.'),
(UUID(), 'LOAN_DISBURSED',              'SMS', 'sw', 'TZS {{amount}} imetumwa akaunti yako. Kumb: {{reference}}. Malipo ya kwanza: {{firstPaymentDate}}.'),
(UUID(), 'CONTRIBUTION_MISSED',         'SMS', 'sw', 'Mchango wako wa mwezi huu haujafika. Tafadhali lipa TZS {{amount}} kabla ya {{dueDate}} kuepuka adhabu.'),
(UUID(), 'GUARANTOR_CONSENT_REQUESTED', 'SMS', 'sw', '{{borrowerName}} amekuweka mdhamini wa mkopo wa TZS {{amount}}. Jibu 1 kukubali, 2 kukataa. Kumb: {{reference}}.'),
(UUID(), 'DIVIDEND_DECLARED',           'SMS', 'sw', 'Gawio la {{year}} limetangazwa. Sehemu yako: TZS {{amount}}. Itaingizwa akaunti tarehe {{paymentDate}}.'),
(UUID(), 'OTP_VERIFICATION',            'SMS', 'sw', 'Nambari yako ya uthibitisho ni {{otp}}. Inatumika kwa dakika 5 peke yake. Usimwambie mtu.'),
(UUID(), 'LOAN_REPAYMENT_REMINDER',     'SMS', 'sw', 'Ukumbusho: Malipo ya mkopo TZS {{amount}} yanastahili tarehe {{dueDate}}. Kumb: {{reference}}.');
