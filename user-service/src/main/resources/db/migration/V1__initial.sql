CREATE SCHEMA IF NOT EXISTS  users;

CREATE USER user_service WITH PASSWORD 'user_service_password';
GRANT USAGE ON SCHEMA users TO user_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA users TO user_service;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA  users TO user_service;
ALTER DEFAULT PRIVILEGES  IN SCHEMA users GRANT ALL ON TABLES TO user_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA users GRANT ALL ON SEQUENCES TO user_service;


CREATE TABLE users.users (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             email VARCHAR(500) UNIQUE NOT NULL ,
                             password_hash VARCHAR(255) NOT NULL ,
                             full_name VARCHAR (255) NOT NULL ,
                             phone VARCHAR(20),
                             role VARCHAR(20) NOT NULL CHECK ( role IN ('OWNER','CUSTOMER','ADMIN') ),
                             kyc_status VARCHAR(20) NOT NULL CHECK ( kyc_status IN ('PENDING','VERIFIED','REJECTED')),
                             business_license VARCHAR(255),
                             profile_image_url VARCHAR(500),
                             verified_at TIMESTAMP,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP DEFAULT  CURRENT_TIMESTAMP

);

CREATE INDEX idx_users_email ON users.users(email);
CREATE INDEX idx_users_role ON users.users(role);