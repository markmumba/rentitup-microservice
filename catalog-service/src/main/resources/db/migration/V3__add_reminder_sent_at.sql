-- Add reminder tracking field to maintenance_records table
ALTER TABLE catalog.maintenance_records
    ADD COLUMN IF NOT EXISTS reminder_sent_at TIMESTAMP;

-- Index for efficiently finding records that need reminders
CREATE INDEX IF NOT EXISTS idx_maintenance_records_reminder
    ON catalog.maintenance_records(next_service_date, reminder_sent_at)
    WHERE next_service_date IS NOT NULL;
