package com.movie.booking.catalog_service.mapper;

import com.movie.booking.catalog_service.dto.TheaterRequest;
import com.movie.booking.catalog_service.dto.TheaterResponse;
import com.movie.booking.catalog_service.entity.Theater;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.UUID;

@Mapper(config = CatalogMapperConfig.class)
public interface TheaterMapper {

    TheaterResponse toResponse(Theater theater);

    Theater toEntity(TheaterRequest request, UUID id, Instant now);

    // Theater has no setters, so this calls its update(...) method.
    default void apply(TheaterRequest request, Theater theater, Instant now) {
        theater.update(request.name(), request.city(), request.address(), now);
    }
}
