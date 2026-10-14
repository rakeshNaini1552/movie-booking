ALTER TABLE booking
    ADD COLUMN idempotency_key UUID,
ADD CONSTRAINT uq_booking_idempotency_key UNIQUE (idempotency_key);