package com.movie.booking.booking_service.excepiton;

import org.springframework.web.client.ResourceAccessException;

public class SeatServiceUnavailableException extends RuntimeException {
    public SeatServiceUnavailableException(String s, Throwable e) {
        super(s, e);
    }
}
