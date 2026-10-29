package com.movie.booking.payment_service.service;

import com.movie.booking.payment_service.entity.PaymentStatus;

import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentStatus pay(String paymentToken, BigDecimal amount);
}
