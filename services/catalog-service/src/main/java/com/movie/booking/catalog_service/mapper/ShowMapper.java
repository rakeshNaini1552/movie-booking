package com.movie.booking.catalog_service.mapper;

import com.movie.booking.catalog_service.dto.ShowRequest;
import com.movie.booking.catalog_service.dto.ShowResponse;
import com.movie.booking.catalog_service.entity.Show;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.UUID;

@Mapper(config = CatalogMapperConfig.class)
public interface ShowMapper {

    ShowResponse toResponse(Show show);

    // The Show constructor sets the status to SCHEDULED itself.
    Show toEntity(ShowRequest request, UUID id, Instant now);

    // Show has no setters, so this calls its update(...) method. Status changes only through cancel().
    default void apply(ShowRequest request, Show show, Instant now) {
        show.update(request.movieId(), request.screenId(), request.startsAt(), request.endsAt(),
                request.basePrice(), now);
    }
}
