CREATE TABLE booking (
                         booking_id    uuid           NOT NULL,
                         user_id       uuid           NOT NULL,
                         show_id       uuid           NOT NULL,
                         status        varchar(20)    NOT NULL,
                         total_amount  numeric(10, 2) NOT NULL,
                         created_at    timestamptz    NOT NULL DEFAULT now(),
                         updated_at    timestamptz    NOT NULL DEFAULT now(),

                         PRIMARY KEY (booking_id),
                         CONSTRAINT booking_status_check
                             CHECK (status IN ('PENDING', 'CONFIRMED', 'FAILED', 'EXPIRED', 'CANCELLED'))
);

CREATE TABLE booking_seat (
                              booking_id  uuid        NOT NULL,
                              seat_id     varchar(3)  NOT NULL,

                              PRIMARY KEY (booking_id, seat_id),
                              FOREIGN KEY (booking_id) REFERENCES booking (booking_id)
);