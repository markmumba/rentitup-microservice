

CREATE UNLOGGED TABLE category_cache(
    id UUID PRIMARY KEY,
    value JSONB NOT NULL,
    inserted_at TIMESTAMP DEFAULT NOW()
);

CREATE UNLOGGED TABLE machine_cache(
    id UUID PRIMARY KEY,
    value JSONB NOT NULL,
    inserted_at TIMESTAMP DEFAULT NOW()
);

CREATE UNLOGGED TABLE machine_images_cache(
    machine_id UUID PRIMARY KEY,
    value JSONB NOT NULL,
    inserted_at TIMESTAMP DEFAULT NOW()
);

CREATE UNLOGGED TABLE maintenance_records_cache (
       machine_id UUID PRIMARY KEY,
       value JSONB NOT NULL,
       inserted_at TIMESTAMP DEFAULT NOW()
);

-- Procedures --

CREATE OR REPLACE PROCEDURE expire_cache (
        target_table  TEXT, 
        retention_period INTERVAL
) AS
$$
BEGIN
    EXECUTE format(
        'DELETE FROM %I WHERE inserted_at < NOW() - $1',
        target_table
    ) USING retention_period;
END;
$$ LANGUAGE plpgsql;

CALL expire_data('category_cache','60 minutes');
CALL expire_data('machine_cache','60 minutes');
CALL expire_data('machine_images_cache','60 minutes');

/*
Then schedule it with pg_cron if you have it, or call it from your Spring app with a @Scheduled method.
*/

/*
*/