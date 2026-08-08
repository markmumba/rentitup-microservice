ALTER TABLE notification.notifications
    ADD COLUMN read_at TIMESTAMP;

CREATE INDEX idx_notifications_user_unread
    ON notification.notifications(user_id, created_at DESC)
    WHERE read_at IS NULL;
