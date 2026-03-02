CREATE SCHEMA IF NOT EXISTS notification;

CREATE USER notification_service WITH PASSWORD 'notification_service_password';
GRANT USAGE ON SCHEMA notification TO notification_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA notification TO notification_service;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA notification TO notification_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA notification GRANT ALL ON TABLES TO notification_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA notification GRANT ALL ON SEQUENCES TO notification_service;

CREATE TABLE notification.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    recipient VARCHAR(255) NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'PUSH')),
    template_key VARCHAR(100) NOT NULL,
    data JSONB DEFAULT '{}',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'DELIVERED')),
    priority VARCHAR(20) DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    subject VARCHAR(500),
    body TEXT,
    error_message VARCHAR(1000),
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_notifications_user ON notification.notifications(user_id);
CREATE INDEX idx_notifications_status ON notification.notifications(status);
CREATE INDEX idx_notifications_channel ON notification.notifications(channel);
CREATE INDEX idx_notifications_template ON notification.notifications(template_key);
CREATE INDEX idx_notifications_created ON notification.notifications(created_at DESC);
CREATE INDEX idx_notifications_retry ON notification.notifications(status, retry_count) WHERE status = 'FAILED';
