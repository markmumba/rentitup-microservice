CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_transaction_id
    ON booking.payments(transaction_id)
    WHERE transaction_id IS NOT NULL;
