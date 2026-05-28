-- ============================================================
-- V1__notification_schema.sql
-- Notification Service — Full Schema
-- ============================================================

-- ── Notifications log ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id              VARCHAR(36)     NOT NULL DEFAULT (UUID()),
    recipient_id    VARCHAR(36)     NOT NULL,   -- userId or memberId
    recipient_phone VARCHAR(20),
    recipient_email VARCHAR(150),
    recipient_fcm   VARCHAR(500),               -- FCM token for push

    channel         VARCHAR(20)     NOT NULL,   -- SMS | PUSH | WHATSAPP | EMAIL
    event_type      VARCHAR(100)    NOT NULL,   -- MEMBER_CREATED | LOAN_APPROVED etc.
    subject         VARCHAR(200),               -- push/email subject
    message         TEXT            NOT NULL,
    language        VARCHAR(5)      NOT NULL DEFAULT 'SW',

    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',  -- PENDING | SENT | FAILED | RETRYING
    attempts        INT             NOT NULL DEFAULT 0,
    max_attempts    INT             NOT NULL DEFAULT 3,
    last_error      VARCHAR(500),
    sent_at         DATETIME,

    tenant_id       VARCHAR(50)     NOT NULL DEFAULT 'default',
    reference_id    VARCHAR(36),                -- loan ID, member ID etc.
    reference_type  VARCHAR(50),                -- LOAN | MEMBER | CONTRIBUTION

    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_notif_recipient   (recipient_id),
    INDEX idx_notif_status      (status, attempts),
    INDEX idx_notif_event       (event_type, tenant_id),
    INDEX idx_notif_created     (created_at),
    INDEX idx_notif_reference   (reference_type, reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Notification templates ─────────────────────────────────
CREATE TABLE IF NOT EXISTS notification_templates (
    id          VARCHAR(36)     NOT NULL DEFAULT (UUID()),
    event_type  VARCHAR(100)    NOT NULL,
    channel     VARCHAR(20)     NOT NULL,
    language    VARCHAR(5)      NOT NULL DEFAULT 'SW',
    subject     VARCHAR(200),
    template    TEXT            NOT NULL,   -- uses {name}, {amount}, {date} placeholders
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_template (event_type, channel, language)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Seed default templates ─────────────────────────────────
INSERT IGNORE INTO notification_templates (event_type, channel, language, template) VALUES

-- Member created
('MEMBER_CREATED', 'SMS', 'SW',
 'Karibu {name}! Umesajiliwa kwa SACCOS. Namba yako ya mwanachama ni {memberNumber}. Wasiliana nasi kwa maswali yoyote.'),
('MEMBER_CREATED', 'SMS', 'EN',
 'Welcome {name}! You have been registered with SACCOS. Your member number is {memberNumber}. Contact us for any queries.'),

-- Loan approved
('LOAN_APPROVED', 'SMS', 'SW',
 'Hongera {name}! Mkopo wako wa TZS {amount} umeidhinishwa. Utapitiwa hivi karibuni.'),
('LOAN_APPROVED', 'PUSH', 'SW',
 'Mkopo wa TZS {amount} umeidhinishwa!'),
('LOAN_APPROVED', 'SMS', 'EN',
 'Congratulations {name}! Your loan of TZS {amount} has been approved. Disbursement will follow shortly.'),

-- Loan rejected
('LOAN_REJECTED', 'SMS', 'SW',
 'Samahani {name}. Ombi lako la mkopo wa TZS {amount} limekataliwa. Sababu: {reason}. Wasiliana na ofisi kwa maelezo zaidi.'),
('LOAN_REJECTED', 'SMS', 'EN',
 'Sorry {name}. Your loan application of TZS {amount} was rejected. Reason: {reason}. Contact the office for details.'),

-- Loan disbursed
('LOAN_DISBURSED', 'SMS', 'SW',
 '{name}, mkopo wako wa TZS {amount} umetumwa kwenye akaunti yako. Tarehe ya kwanza kulipa: {dueDate}.'),
('LOAN_DISBURSED', 'PUSH', 'SW',
 'TZS {amount} imetumwa kwenye akaunti yako!'),

-- Contribution missed
('CONTRIBUTION_MISSED', 'SMS', 'SW',
 '{name}, bado haujalipia mchango wa mwezi huu (TZS {amount}). Tafadhali lipa kabla ya {dueDate} kuepuka adhabu.'),

-- Guarantor consent
('GUARANTOR_CONSENT_REQUESTED', 'WHATSAPP', 'SW',
 'Habari {name}! Umeitwa kuwa mdhamini wa {borrowerName} katika mkopo wa TZS {amount}. Jibu YES kukubali au NO kukataa.'),
('GUARANTOR_CONSENT_REQUESTED', 'SMS', 'SW',
 '{name}, {borrowerName} anakuomba udhamini wa mkopo wa TZS {amount}. Piga simu ofisi kukubali au kukataa.'),

-- Dividend declared
('DIVIDEND_DECLARED', 'SMS', 'SW',
 'Habari njema {name}! Gawio la TZS {amount} limetangazwa. Utapata TZS {yourShare} kulingana na hisa zako {shares}.'),
('DIVIDEND_DECLARED', 'PUSH', 'SW',
 'Gawio la TZS {yourShare} limetangazwa!');
