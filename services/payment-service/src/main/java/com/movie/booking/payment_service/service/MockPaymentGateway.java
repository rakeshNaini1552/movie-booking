package com.movie.booking.payment_service.service;

import com.movie.booking.payment_service.entity.PaymentStatus;
import com.movie.booking.payment_service.exception.InvalidPaymentTokenException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class MockPaymentGateway implements PaymentGateway{
    @Override
    public PaymentStatus pay(String paymentToken, BigDecimal amount) {
        if(Objects.equals(paymentToken, "tok_success")) return PaymentStatus.SUCCESS;
        else if(Objects.equals(paymentToken, "tok_decline")) return PaymentStatus.FAILED;
        else throw new InvalidPaymentTokenException("Unknown payment token");
    }
}
