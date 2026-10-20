CREATE TABLE ticket (
                        ticket_id   uuid         NOT NULL,
                        booking_id  uuid         NOT NULL,
                        event_id    uuid         NOT NULL,
                        qr_payload  text         NOT NULL,
                        status      varchar(20)  NOT NULL,
                        issued_at   timestamptz  NOT NULL DEFAULT now(),
                        used_at     timestamptz,

                        PRIMARY KEY (ticket_id),
                        CONSTRAINT ticket_booking_id_unique UNIQUE (booking_id),
                        CONSTRAINT ticket_status_check CHECK (status IN ('ISSUED', 'USED', 'VOID'))
);