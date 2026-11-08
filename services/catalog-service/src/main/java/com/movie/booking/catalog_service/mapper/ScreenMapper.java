package com.movie.booking.catalog_service.mapper;

import com.movie.booking.catalog_service.dto.ScreenRequest;
import com.movie.booking.catalog_service.dto.ScreenResponse;
import com.movie.booking.catalog_service.entity.Screen;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.UUID;

@Mapper(config = CatalogMapperConfig.class)
public interface ScreenMapper {

    ScreenResponse toResponse(Screen screen);

    Screen toEntity(UUID theaterId, ScreenRequest request, UUID id, Instant now);

    // Screen has no setters, so this calls its update(...) method. The theater never changes.
    default void apply(ScreenRequest request, Screen screen, Instant now) {
        screen.update(request.name(), request.seatRows(), request.seatsPerRow(), now);
    }
}
