CREATE TABLE outbox (
                        id           UUID PRIMARY KEY,
                        aggregate_id UUID NOT NULL,
                        event_type   VARCHAR(100) NOT NULL,
                        topic        VARCHAR(100) NOT NULL,
                        payload      TEXT NOT NULL,
                        created_at   timestamptz NOT NULL,
                        sent_at      timestamptz
);

CREATE INDEX idx_outbox_unsent ON outbox (created_at) WHERE sent_at IS NULL;