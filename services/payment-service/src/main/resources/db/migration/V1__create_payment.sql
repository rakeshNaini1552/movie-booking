CREATE TABLE payment (
                         payment_id      UUID PRIMARY KEY,
                         booking_id      UUID NOT NULL,
                         amount          NUMERIC(10,2) NOT NULL,
                         status          VARCHAR(20) NOT NULL,
                         idempotency_key UUID NOT NULL UNIQUE ,
                         created_at      TIMESTAMPTZ NOT NULL,
                         updated_at      TIMESTAMPTZ NOT NULL
);
