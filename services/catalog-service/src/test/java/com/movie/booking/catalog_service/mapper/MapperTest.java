package com.movie.booking.catalog_service.mapper;

import com.movie.booking.catalog_service.MovieStatus;
import com.movie.booking.catalog_service.ShowStatus;
import com.movie.booking.catalog_service.dto.MovieRequest;
import com.movie.booking.catalog_service.dto.MovieResponse;
import com.movie.booking.catalog_service.dto.ScreenRequest;
import com.movie.booking.catalog_service.dto.ShowRequest;
import com.movie.booking.catalog_service.dto.TheaterRequest;
import com.movie.booking.catalog_service.entity.Movie;
import com.movie.booking.catalog_service.entity.Screen;
import com.movie.booking.catalog_service.entity.Show;
import com.movie.booking.catalog_service.entity.Theater;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MapperTest {

    private static final Instant T0 = Instant.parse("2030-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2030-02-01T00:00:00Z");

    private final MovieMapper movieMapper = new MovieMapperImpl();
    private final TheaterMapper theaterMapper = new TheaterMapperImpl();
    private final ScreenMapper screenMapper = new ScreenMapperImpl();
    private final ShowMapper showMapper = new ShowMapperImpl();

    @Test
    void movie_create_without_status_defaults_to_active_and_stamps_id_and_time() {
        UUID id = UUID.randomUUID();

        Movie movie = movieMapper.toEntity(new MovieRequest("Dune", null, 155, "en", null, null, null), id, T0);

        assertThat(movie.getId()).isEqualTo(id);
        assertThat(movie.getStatus()).isEqualTo(MovieStatus.ACTIVE);
        assertThat(movie.getDurationMinutes()).isEqualTo(155);
        assertThat(movie.getDescription()).isNull();
        assertThat(movie.getCreatedAt()).isEqualTo(T0);
        assertThat(movie.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    void movie_create_keeps_a_given_status() {
        Movie movie = movieMapper.toEntity(
                new MovieRequest("Dune", "d", 155, "en", "PG", LocalDate.of(2021, 10, 22), MovieStatus.ARCHIVED),
                UUID.randomUUID(), T0);

        assertThat(movie.getStatus()).isEqualTo(MovieStatus.ARCHIVED);
        assertThat(movie.getReleaseDate()).isEqualTo(LocalDate.of(2021, 10, 22));
    }

    @Test
    void movie_update_without_status_keeps_the_current_status_and_only_moves_updatedAt() {
        Movie movie = movieMapper.toEntity(
                new MovieRequest("Dune", null, 155, "en", null, null, MovieStatus.ARCHIVED), UUID.randomUUID(), T0);

        movieMapper.apply(new MovieRequest("Dune 2", "new", 166, "fr", "R", null, null), movie, T1);

        assertThat(movie.getStatus()).isEqualTo(MovieStatus.ARCHIVED);
        assertThat(movie.getTitle()).isEqualTo("Dune 2");
        assertThat(movie.getDurationMinutes()).isEqualTo(166);
        assertThat(movie.getCreatedAt()).isEqualTo(T0);
        assertThat(movie.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void movie_update_with_status_replaces_it() {
        Movie movie = movieMapper.toEntity(new MovieRequest("Dune", null, 155, "en", null, null, null),
                UUID.randomUUID(), T0);

        movieMapper.apply(new MovieRequest("Dune", null, 155, "en", null, null, MovieStatus.ARCHIVED), movie, T1);

        assertThat(movie.getStatus()).isEqualTo(MovieStatus.ARCHIVED);
    }

    @Test
    void movie_response_copies_every_field_and_null_maps_to_null() {
        Movie movie = movieMapper.toEntity(new MovieRequest("Dune", null, 155, "en", null, null, null),
                UUID.randomUUID(), T0);

        MovieResponse response = movieMapper.toResponse(movie);

        assertThat(response.id()).isEqualTo(movie.getId());
        assertThat(response.title()).isEqualTo("Dune");
        assertThat(response.status()).isEqualTo(MovieStatus.ACTIVE);
        assertThat(response.createdAt()).isEqualTo(T0);
        assertThat(movieMapper.toResponse(null)).isNull();
    }

    @Test
    void theater_create_update_and_response() {
        UUID id = UUID.randomUUID();
        Theater theater = theaterMapper.toEntity(new TheaterRequest("PVR", "Austin", "1 Main"), id, T0);

        theaterMapper.apply(new TheaterRequest("PVR 2", "Dallas", "2 Main"), theater, T1);

        var response = theaterMapper.toResponse(theater);
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("PVR 2");
        assertThat(response.city()).isEqualTo("Dallas");
        assertThat(response.createdAt()).isEqualTo(T0);
        assertThat(response.updatedAt()).isEqualTo(T1);
        assertThat(theaterMapper.toResponse(null)).isNull();
    }

    @Test
    void screen_create_takes_the_theater_id_and_update_keeps_it() {
        UUID theaterId = UUID.randomUUID();
        Screen screen = screenMapper.toEntity(theaterId, new ScreenRequest("S1", 10, 12), UUID.randomUUID(), T0);

        screenMapper.apply(new ScreenRequest("S2", 8, 9), screen, T1);

        assertThat(screen.getTheaterId()).isEqualTo(theaterId);
        assertThat(screen.getName()).isEqualTo("S2");
        assertThat(screen.getSeatRows()).isEqualTo(8);
        assertThat(screen.getSeatsPerRow()).isEqualTo(9);
        assertThat(screen.getCreatedAt()).isEqualTo(T0);
        assertThat(screen.getUpdatedAt()).isEqualTo(T1);
        assertThat(screenMapper.toResponse(screen).theaterId()).isEqualTo(theaterId);
        assertThat(screenMapper.toResponse(null)).isNull();
    }

    @Test
    void show_create_is_scheduled_and_update_leaves_status_alone() {
        UUID movieId = UUID.randomUUID();
        UUID screenId = UUID.randomUUID();
        Instant start = T1;
        Instant end = T1.plusSeconds(7200);
        Show show = showMapper.toEntity(new ShowRequest(movieId, screenId, start, end, new BigDecimal("12.50")),
                UUID.randomUUID(), T0);
        assertThat(show.getStatus()).isEqualTo(ShowStatus.SCHEDULED);
        show.cancel(T0);

        showMapper.apply(new ShowRequest(movieId, screenId, start.plusSeconds(60), end, new BigDecimal("9.00")),
                show, T1);

        var response = showMapper.toResponse(show);
        assertThat(response.status()).isEqualTo(ShowStatus.CANCELLED);
        assertThat(response.basePrice()).isEqualByComparingTo("9.00");
        assertThat(response.startsAt()).isEqualTo(start.plusSeconds(60));
        assertThat(response.createdAt()).isEqualTo(T0);
        assertThat(response.updatedAt()).isEqualTo(T1);
        assertThat(showMapper.toResponse(null)).isNull();
    }
}
