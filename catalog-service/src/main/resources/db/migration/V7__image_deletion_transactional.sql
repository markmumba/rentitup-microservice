
CREATE TABLE catalog.minio_outbox (
    id UUID PRIMARY KEY DEFAULT  gen_random_uuid(),
    object_key VARCHAR(500) NOT NULL ,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK ( status IN ('PENDING','PROCESSING','DELETED') ),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_object_key_to_delete ON catalog.minio_outbox(object_key);
