CREATE TABLE show_seat (
                           show_id       uuid  NOT NULL,
                           seat_id       varchar(3)  NOT NULL,
                           seat_row      varchar(1)  NOT NULL,
                           seat_number   integer  NOT NULL,
                           seat_type     varchar(20)  NOT NULL,
                           status        varchar(20)  NOT NULL DEFAULT 'AVAILABLE',
                           booking_id    uuid,
                           version       bigint  NOT NULL DEFAULT 0,
                           updated_at    timestamp  NOT NULL DEFAULT now(),

                           PRIMARY KEY (show_id, seat_id),
                           CONSTRAINT show_seat_status_check
                               CHECK (status IN ('AVAILABLE', 'BOOKED', 'BLOCKED'))
);