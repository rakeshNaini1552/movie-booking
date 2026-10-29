package com.movie.booking.payment_service.repository;

import com.movie.booking.payment_service.entity.Payment;
import com.movie.booking.payment_service.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(UUID idempotencyKey);

    @Modifying(clearAutomatically = true)
    @Query("update Payment p set p.status = :newStatus, p.updatedAt = :now "
            + "where p.bookingId = :bookingId and p.status = :expected")
    int transitionByBookingId(@Param("bookingId") UUID bookingId,
                              @Param("expected") PaymentStatus expected,
                              @Param("newStatus") PaymentStatus newStatus,
                              @Param("now") Instant now);
}
