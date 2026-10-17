package com.movie.booking.booking_service.client;

import com.movie.booking.booking_service.dto.PaymentRequest;
import com.movie.booking.booking_service.dto.PaymentResult;
import com.movie.booking.booking_service.exception.InvalidPaymentTokenException;
import com.movie.booking.booking_service.exception.PaymentServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentServiceClient implements PaymentClient{

    private final RestClient paymentServiceRestClient;

    @Override
    public PaymentResult pay(UUID bookingId, BigDecimal amount, String paymentToken, UUID idempotencyKey) {
        try {
            return paymentServiceRestClient.post()
                    .uri("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", idempotencyKey.toString())
                    .body(new PaymentRequest(bookingId, amount, paymentToken))
                    .retrieve()
                    .body(PaymentResult.class);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new InvalidPaymentTokenException("...");
        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new PaymentServiceUnavailableException("...", e);
        }
    }
}
