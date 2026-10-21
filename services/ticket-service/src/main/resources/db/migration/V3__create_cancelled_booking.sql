CREATE TABLE cancelled_booking (
                                   booking_id    uuid         NOT NULL,
                                   cancelled_at  timestamptz  NOT NULL,

                                   PRIMARY KEY (booking_id)
);
