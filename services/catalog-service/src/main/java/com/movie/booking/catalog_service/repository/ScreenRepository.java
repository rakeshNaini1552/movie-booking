package com.movie.booking.catalog_service.repository;

import com.movie.booking.catalog_service.entity.Screen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScreenRepository extends JpaRepository<Screen, UUID> {

    Page<Screen> findByTheaterId(UUID theaterId, Pageable pageable);

    Optional<Screen> findByIdAndTheaterId(UUID id, UUID theaterId);

    boolean existsByTheaterIdAndName(UUID theaterId, String name);

    boolean existsByTheaterIdAndNameAndIdNot(UUID theaterId, String name, UUID id);

    void deleteByTheaterId(UUID theaterId);
}
