-- show.id is uuid, the same type as show_seat.show_id in seat-service, so a catalog show can be referenced there later.
CREATE TABLE movie (
    id               uuid          PRIMARY KEY,
    title            varchar(255)  NOT NULL,
    description      text,
    duration_minutes integer       NOT NULL,
    language         varchar(50)   NOT NULL,
    rating           varchar(10),
    release_date     date,
    status           varchar(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at       timestamptz   NOT NULL,
    updated_at       timestamptz   NOT NULL,

    CONSTRAINT movie_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT movie_duration_check CHECK (duration_minutes > 0)
);

CREATE INDEX idx_movie_status ON movie (status);
CREATE INDEX idx_movie_title_lower ON movie (lower(title));

CREATE TABLE theater (
    id         uuid          PRIMARY KEY,
    name       varchar(255)  NOT NULL,
    city       varchar(100)  NOT NULL,
    address    varchar(500)  NOT NULL,
    created_at timestamptz   NOT NULL,
    updated_at timestamptz   NOT NULL
);

CREATE INDEX idx_theater_city ON theater (city);

CREATE TABLE screen (
    id             uuid          PRIMARY KEY,
    theater_id     uuid          NOT NULL REFERENCES theater (id),
    name           varchar(100)  NOT NULL,
    seat_rows      integer       NOT NULL,
    seats_per_row  integer       NOT NULL,
    created_at     timestamptz   NOT NULL,
    updated_at     timestamptz   NOT NULL,

    CONSTRAINT screen_theater_name_unique UNIQUE (theater_id, name),
    CONSTRAINT screen_seat_rows_check CHECK (seat_rows BETWEEN 1 AND 26),
    CONSTRAINT screen_seats_per_row_check CHECK (seats_per_row BETWEEN 1 AND 99)
);

CREATE TABLE show (
    id         uuid           PRIMARY KEY,
    movie_id   uuid           NOT NULL REFERENCES movie (id),
    screen_id  uuid           NOT NULL REFERENCES screen (id),
    starts_at  timestamptz    NOT NULL,
    ends_at    timestamptz    NOT NULL,
    base_price numeric(10, 2) NOT NULL,
    status     varchar(20)    NOT NULL DEFAULT 'SCHEDULED',
    created_at timestamptz    NOT NULL,
    updated_at timestamptz    NOT NULL,

    CONSTRAINT show_screen_starts_unique UNIQUE (screen_id, starts_at),
    CONSTRAINT show_status_check CHECK (status IN ('SCHEDULED', 'CANCELLED')),
    CONSTRAINT show_time_order_check CHECK (ends_at > starts_at),
    CONSTRAINT show_base_price_check CHECK (base_price >= 0)
);

-- (screen_id, starts_at) is already indexed by the unique constraint; these cover the list filters and the delete checks.
CREATE INDEX idx_show_movie_starts ON show (movie_id, starts_at);
CREATE INDEX idx_show_starts_at ON show (starts_at);
