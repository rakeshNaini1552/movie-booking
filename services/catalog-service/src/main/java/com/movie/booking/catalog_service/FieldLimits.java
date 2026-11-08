package com.movie.booking.catalog_service;

// Mirror the column sizes and CHECK constraints in V1__create_catalog.sql; keep both in step.
public final class FieldLimits {

    public static final int TITLE = 255;
    public static final int LANGUAGE = 50;
    public static final int RATING = 10;
    public static final int THEATER_NAME = 255;
    public static final int CITY = 100;
    public static final int ADDRESS = 500;
    public static final int SCREEN_NAME = 100;
    // seat-service identifies a seat as one row letter plus up to two digits (show_seat.seat_id is varchar(3)).
    public static final int MAX_SEAT_ROWS = 26;
    public static final int MAX_SEATS_PER_ROW = 99;

    private FieldLimits() {}
}
