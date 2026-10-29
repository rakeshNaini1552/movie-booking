package com.movie.booking.payment_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

public enum PaymentStatus {
    SUCCESS, FAILED, REFUNDED;
}
