package com.movie.booking.booking_service.repository;

import com.movie.booking.booking_service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    long countBySentAtIsNull();

    List<OutboxEvent> findTop100BySentAtIsNullOrderByCreatedAtAsc();

}
