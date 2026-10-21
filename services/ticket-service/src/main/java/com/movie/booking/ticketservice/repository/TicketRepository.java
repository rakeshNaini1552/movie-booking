package com.movie.booking.ticketservice.repository;

import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findByBookingId(UUID bookingId);

    @Modifying(clearAutomatically = true)
    @Query("update Ticket t set t.status = :newStatus, t.usedAt = :now "
            + "where t.ticketId = :id and t.status = :expected")
    int markUsed(@Param("id") UUID id,
                 @Param("expected") TicketStatus expected,
                 @Param("newStatus") TicketStatus newStatus,
                 @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("update Ticket t set t.status = :newStatus "
            + "where t.bookingId = :bookingId and t.status = :expected")
    int voidIfIssued(@Param("bookingId") UUID bookingId,
                     @Param("expected") TicketStatus expected,
                     @Param("newStatus") TicketStatus newStatus);
}
