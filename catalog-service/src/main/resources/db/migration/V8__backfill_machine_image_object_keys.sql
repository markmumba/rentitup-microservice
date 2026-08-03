-- Backfill objects created by the existing local MinIO URL format.
UPDATE catalog.machine_images
SET object_key = substring(url FROM '/rentitup-images/(.*)$')
WHERE object_key IS NULL
  AND url LIKE '%/rentitup-images/%';

CREATE UNIQUE INDEX IF NOT EXISTS idx_machine_images_object_key
    ON catalog.machine_images(object_key)
    WHERE object_key IS NOT NULL;
