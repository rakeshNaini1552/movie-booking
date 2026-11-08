package com.movie.booking.catalog_service.service;

import com.movie.booking.catalog_service.MovieStatus;
import com.movie.booking.catalog_service.dto.MovieRequest;
import com.movie.booking.catalog_service.dto.MovieResponse;
import com.movie.booking.catalog_service.entity.Movie;
import com.movie.booking.catalog_service.exception.ConflictException;
import com.movie.booking.catalog_service.exception.ResourceNotFoundException;
import com.movie.booking.catalog_service.mapper.MovieMapperImpl;
import com.movie.booking.catalog_service.repository.MovieRepository;
import com.movie.booking.catalog_service.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @Mock private MovieRepository movieRepository;
    @Mock private ShowRepository showRepository;

    private MovieService service;

    @BeforeEach
    void setUp() {
        service = new MovieService(movieRepository, showRepository, new MovieMapperImpl(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Movie movie(MovieStatus status) {
        return Movie.builder().id(UUID.randomUUID()).title("Dune").durationMinutes(155).language("en")
                .status(status).now(NOW).build();
    }

    @Test
    void create_defaults_the_status_to_active() {
        when(movieRepository.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        MovieResponse response = service.create(
                new MovieRequest("Dune", null, 155, "en", null, null, null));

        assertThat(response.status()).isEqualTo(MovieStatus.ACTIVE);
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void archive_with_shows_is_a_conflict_and_leaves_the_movie_active() {
        Movie movie = movie(MovieStatus.ACTIVE);
        when(movieRepository.findById(movie.getId())).thenReturn(Optional.of(movie));
        when(showRepository.existsByMovieId(movie.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.archive(movie.getId())).isInstanceOf(ConflictException.class);
        assertThat(movie.getStatus()).isEqualTo(MovieStatus.ACTIVE);
    }

    @Test
    void archive_without_shows_sets_archived() {
        Movie movie = movie(MovieStatus.ACTIVE);
        when(movieRepository.findById(movie.getId())).thenReturn(Optional.of(movie));
        when(showRepository.existsByMovieId(movie.getId())).thenReturn(false);

        service.archive(movie.getId());

        assertThat(movie.getStatus()).isEqualTo(MovieStatus.ARCHIVED);
    }

    @Test
    void update_without_status_keeps_the_current_status() {
        Movie movie = movie(MovieStatus.ARCHIVED);
        when(movieRepository.findById(movie.getId())).thenReturn(Optional.of(movie));

        MovieResponse response = service.update(movie.getId(),
                new MovieRequest("Dune: Part Two", null, 166, "en", null, null, null));

        assertThat(response.title()).isEqualTo("Dune: Part Two");
        assertThat(response.status()).isEqualTo(MovieStatus.ARCHIVED);
    }

    @Test
    void get_of_an_unknown_movie_is_not_found() {
        UUID id = UUID.randomUUID();
        when(movieRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(ResourceNotFoundException.class);
    }
}
