package com.movie.booking.catalog_service.mapper;

import com.movie.booking.catalog_service.MovieStatus;
import com.movie.booking.catalog_service.dto.*;
import com.movie.booking.catalog_service.entity.Movie;
import com.movie.booking.catalog_service.entity.Screen;
import com.movie.booking.catalog_service.entity.Show;
import com.movie.booking.catalog_service.entity.Theater;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

// Hand-written mapping kept in one place so it can be swapped for MapStruct later.
@Component
public class CatalogMapper {

    public Movie toEntity(MovieRequest request, Instant now) {
        return Movie.builder()
                .id(UUID.randomUUID())
                .title(request.title())
                .description(request.description())
                .durationMinutes(request.durationMinutes())
                .language(request.language())
                .rating(request.rating())
                .releaseDate(request.releaseDate())
                .status(request.status() != null ? request.status() : MovieStatus.ACTIVE)
                .now(now)
                .build();
    }

    public void apply(MovieRequest request, Movie movie, Instant now) {
        movie.update(request.title(), request.description(), request.durationMinutes(), request.language(),
                request.rating(), request.releaseDate(),
                request.status() != null ? request.status() : movie.getStatus(), now);
    }

    public MovieResponse toResponse(Movie m) {
        return new MovieResponse(m.getId(), m.getTitle(), m.getDescription(), m.getDurationMinutes(),
                m.getLanguage(), m.getRating(), m.getReleaseDate(), m.getStatus(),
                m.getCreatedAt(), m.getUpdatedAt());
    }

    public Theater toEntity(TheaterRequest request, Instant now) {
        return Theater.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .city(request.city())
                .address(request.address())
                .now(now)
                .build();
    }

    public void apply(TheaterRequest request, Theater theater, Instant now) {
        theater.update(request.name(), request.city(), request.address(), now);
    }

    public TheaterResponse toResponse(Theater t) {
        return new TheaterResponse(t.getId(), t.getName(), t.getCity(), t.getAddress(),
                t.getCreatedAt(), t.getUpdatedAt());
    }

    public Screen toEntity(UUID theaterId, ScreenRequest request, Instant now) {
        return Screen.builder()
                .id(UUID.randomUUID())
                .theaterId(theaterId)
                .name(request.name())
                .seatRows(request.seatRows())
                .seatsPerRow(request.seatsPerRow())
                .now(now)
                .build();
    }

    public void apply(ScreenRequest request, Screen screen, Instant now) {
        screen.update(request.name(), request.seatRows(), request.seatsPerRow(), now);
    }

    public ScreenResponse toResponse(Screen s) {
        return new ScreenResponse(s.getId(), s.getTheaterId(), s.getName(), s.getSeatRows(),
                s.getSeatsPerRow(), s.getCreatedAt(), s.getUpdatedAt());
    }

    public Show toEntity(ShowRequest request, Instant now) {
        return Show.builder()
                .id(UUID.randomUUID())
                .movieId(request.movieId())
                .screenId(request.screenId())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .basePrice(request.basePrice())
                .now(now)
                .build();
    }

    public void apply(ShowRequest request, Show show, Instant now) {
        show.update(request.movieId(), request.screenId(), request.startsAt(), request.endsAt(),
                request.basePrice(), now);
    }

    public ShowResponse toResponse(Show s) {
        return new ShowResponse(s.getId(), s.getMovieId(), s.getScreenId(), s.getStartsAt(), s.getEndsAt(),
                s.getBasePrice(), s.getStatus(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
