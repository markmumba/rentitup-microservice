-- Status is the lifecycle source of truth for rows created before both fields were synchronized.
UPDATE catalog.machines
SET is_available = (status = 'AVAILABLE')
WHERE is_available IS DISTINCT FROM (status = 'AVAILABLE');
