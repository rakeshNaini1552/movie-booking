package com.movie.booking.catalog_service.mapper;

import com.movie.booking.catalog_service.dto.MovieRequest;
import com.movie.booking.catalog_service.dto.MovieResponse;
import com.movie.booking.catalog_service.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.UUID;

@Mapper(config = CatalogMapperConfig.class)
public interface MovieMapper {

    MovieResponse toResponse(Movie movie);

    // id and now come from the caller (random UUID and the injected Clock), not from MapStruct.
    @Mapping(target = "status", source = "request.status", defaultValue = "ACTIVE")
    Movie toEntity(MovieRequest request, UUID id, Instant now);

    // Movie has no setters, so this calls its update(...) method. No status in the request keeps the current one.
    default void apply(MovieRequest request, Movie movie, Instant now) {
        movie.update(request.title(), request.description(), request.durationMinutes(), request.language(),
                request.rating(), request.releaseDate(),
                request.status() != null ? request.status() : movie.getStatus(), now);
    }
}
