DROP INDEX IF EXISTS catalog.idx_machines_available;
ALTER TABLE catalog.machines DROP COLUMN IF EXISTS is_available;
