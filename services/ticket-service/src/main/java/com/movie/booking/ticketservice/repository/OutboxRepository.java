package com.movie.booking.ticketservice.repository;

import com.movie.booking.ticketservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop100BySentAtIsNullOrderByCreatedAtAsc();

}
