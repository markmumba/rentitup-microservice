-- Create schema for catalog service
CREATE SCHEMA IF NOT EXISTS catalog;

CREATE USER catalog_service WITH PASSWORD 'catalog_service_password';
GRANT USAGE ON SCHEMA catalog TO catalog_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA catalog TO catalog_service;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA catalog TO catalog_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA catalog GRANT ALL ON TABLES TO catalog_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA catalog GRANT ALL ON SEQUENCES TO catalog_service;

CREATE TABLE catalog.categories (
    id  UUID PRIMARY KEY DEFAULT   gen_random_uuid(),
    name VARCHAR(255) NOT NULL ,
    description TEXT,
    icon_url VARCHAR(500),
    default_price_type VARCHAR(20) DEFAULT 'DAILY' CHECK ( default_price_type IN ('HOURLY','DAILY','WEEKLY','DISTANCE_BASED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE catalog.machines (
    id UUID PRIMARY KEY DEFAULT  gen_random_uuid(),
    owner_id UUID NOT NULL ,
    category_id UUID NOT NULL REFERENCES catalog.categories(id),
    name  VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    base_price DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'KES',
    price_type VARCHAR(20) DEFAULT 'DAILY',
    condition  VARCHAR(20) NOT NULL CHECK ( condition IN ('EXCELLENT','GOOD','FAIR')),
    status VARCHAR(20) DEFAULT 'AVAILABLE' CHECK ( status IN ('AVAILABLE', 'RENTED', 'MAINTENANCE', 'INACTIVE')),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    address VARCHAR(500),
    city VARCHAR(100),
    specifications JSONB DEFAULT '{}',
    is_available BOOLEAN DEFAULT true,
    average_rating DECIMAL(2,1) DEFAULT 0,
    total_reviews INT DEFAULT 0,
    total_rentals INT DEFAULT 0,
    deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX  idx_machine_owner ON catalog.machines(owner_id);
CREATE INDEX idx_machines_category ON catalog.machines(category_id);
CREATE INDEX idx_machines_status ON catalog.machines(status);
CREATE INDEX idx_machines_location ON catalog.machines(latitude,longitude);
CREATE INDEX idx_machines_available ON catalog.machines(is_available,deleted);


CREATE TABLE catalog.machine_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id UUID NOT NULL REFERENCES catalog.machines(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN DEFAULT false,
    display_order INT DEFAULT 0,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);

CREATE INDEX idx_machine_images_machine ON catalog.machine_images(machine_id);

CREATE TABLE catalog.maintenance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id UUID NOT NULL REFERENCES catalog.machines(id) ON DELETE CASCADE,
    service_date DATE NOT NULL,
    description TEXT,
    performed_by VARCHAR(255),
    next_service_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE  INDEX idx_maintenance_machine ON catalog.maintenance_records(machine_id);