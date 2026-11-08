package com.movie.booking.catalog_service.repository;

import com.movie.booking.catalog_service.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TheaterRepository extends JpaRepository<Theater, UUID>, JpaSpecificationExecutor<Theater> {
}
