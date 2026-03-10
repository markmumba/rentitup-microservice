ALTER TABLE booking.reviews
    ADD COLUMN IF NOT EXISTS rating_synced BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS synced_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_reviews_rating_synced ON booking.reviews(rating_synced) WHERE rating_synced = FALSE;
