
CREATE SCHEMA IF NOT EXISTS booking;

CREATE USER booking_service WITH PASSWORD 'booking_service_password';
GRANT USAGE ON SCHEMA booking TO booking_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA booking TO booking_service;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA booking TO booking_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA booking GRANT ALL ON TABLES TO booking_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA booking GRANT ALL ON SEQUENCES TO booking_service;


CREATE TABLE booking.bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    owner_id UUID NOT NULL ,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    duration_days INT NOT NULL,
    pickup_latitude DECIMAL(10, 8),
    pickup_longitude DECIMAL(11, 8),
    pickup_address VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'PAID', 'ONGOING', 'COMPLETED', 'CANCELLED', 'REJECTED')),
    cancellation_reason TEXT,
    daily_rate DECIMAL(12, 2) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    security_deposit DECIMAL(12, 2),
    amount_paid DECIMAL(12, 2) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'KES',
    special_requirements TEXT,
    confirmed_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_dates CHECK (end_date >= start_date)
);


CREATE INDEX idx_bookings_machine ON booking.bookings(machine_id);
CREATE INDEX idx_bookings_customer ON booking.bookings(customer_id);
CREATE INDEX idx_bookings_owner ON booking.bookings(owner_id);
CREATE INDEX idx_bookings_status ON booking.bookings(status);
CREATE INDEX idx_bookings_dates ON booking.bookings(start_date,end_date);


CREATE TABLE booking.payments(
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 booking_id UUID NOT NULL REFERENCES booking.bookings(id),
                                 amount DECIMAL(12,2) NOT NULL,
                                 currency VARCHAR(3) DEFAULT 'KES',
                                 payment_type VARCHAR(20) NOT NULL CHECK ( payment_type IN ('DEPOSIT','FULL_PAYMENT','SECURITY_DEPOSIT')),
                                 status VARCHAR(20) NOT NULL CHECK ( status IN ('PENDING','COMPLETED','FAILED')),
                                 method VARCHAR(20) NOT NULL CHECK ( method IN ('BANK_TRANSFER','MPESA','CARD','CASH')),
                                 transaction_id VARCHAR(255),
                                 mpesa_receipt VARCHAR(255),
                                 completed_at TIMESTAMP,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);


CREATE INDEX idx_payments_bookings ON booking.payments(booking_id);
CREATE INDEX idx_payemnts_status ON booking.payments(status);



CREATE TABLE booking.reviews (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 booking_id UUID UNIQUE NOT NULL REFERENCES booking.bookings(id),  -- One review per booking
                                 reviewer_id UUID NOT NULL,    -- References users.users but NO FK
                                 machine_id UUID NOT NULL,     -- Denormalized for easier queries
                                 owner_id UUID NOT NULL,       -- Denormalized for easier queries
                                 machine_rating INT NOT NULL CHECK (machine_rating BETWEEN 1 AND 5),
                                 owner_rating INT NOT NULL CHECK (owner_rating BETWEEN 1 AND 5),
                                 comment TEXT,
                                 owner_response TEXT,
                                 owner_response_at TIMESTAMP,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reviews_machine ON booking.reviews(machine_id);
CREATE INDEX idx_reviews_owner ON booking.reviews(owner_id);
CREATE INDEX idx_reviews_reviewer ON booking.reviews(reviewer_id);