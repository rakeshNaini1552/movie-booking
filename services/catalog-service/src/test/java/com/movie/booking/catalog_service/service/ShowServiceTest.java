package com.movie.booking.catalog_service.service;

import com.movie.booking.catalog_service.MovieStatus;
import com.movie.booking.catalog_service.ShowStatus;
import com.movie.booking.catalog_service.dto.ShowRequest;
import com.movie.booking.catalog_service.dto.ShowResponse;
import com.movie.booking.catalog_service.entity.Movie;
import com.movie.booking.catalog_service.entity.Show;
import com.movie.booking.catalog_service.exception.ConflictException;
import com.movie.booking.catalog_service.exception.InvalidRequestException;
import com.movie.booking.catalog_service.exception.ResourceNotFoundException;
import com.movie.booking.catalog_service.mapper.ShowMapperImpl;
import com.movie.booking.catalog_service.repository.MovieRepository;
import com.movie.booking.catalog_service.repository.ScreenRepository;
import com.movie.booking.catalog_service.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
    private static final Instant START = Instant.parse("2030-02-01T18:00:00Z");
    private static final Instant END = Instant.parse("2030-02-01T20:00:00Z");

    @Mock private ShowRepository showRepository;
    @Mock private MovieRepository movieRepository;
    @Mock private ScreenRepository screenRepository;

    private ShowService service;
    private final UUID movieId = UUID.randomUUID();
    private final UUID screenId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ShowService(showRepository, movieRepository, screenRepository, new ShowMapperImpl(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ShowRequest request(Instant startsAt, Instant endsAt) {
        return new ShowRequest(movieId, screenId, startsAt, endsAt, new BigDecimal("12.50"));
    }

    private Movie movie(MovieStatus status) {
        return Movie.builder().id(movieId).title("Dune").durationMinutes(155).language("en")
                .status(status).now(NOW).build();
    }

    private void movieAndScreenExist() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie(MovieStatus.ACTIVE)));
        when(screenRepository.existsById(screenId)).thenReturn(true);
    }

    @Test
    void create_saves_the_show_when_nothing_overlaps() {
        movieAndScreenExist();
        when(showRepository.existsOverlapping(any(), any(), any(), any())).thenReturn(false);
        when(showRepository.save(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

        ShowResponse response = service.create(request(START, END));

        assertThat(response.status()).isEqualTo(ShowStatus.SCHEDULED);
        assertThat(response.startsAt()).isEqualTo(START);
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void create_rejects_a_show_overlapping_another_on_the_same_screen() {
        movieAndScreenExist();
        when(showRepository.existsOverlapping(any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(START, END)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlapping");
        verify(showRepository, never()).save(any());
    }

    @Test
    void create_rejects_end_not_after_start() {
        assertThatThrownBy(() -> service.create(request(END, START)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.create(request(START, START)))
                .isInstanceOf(InvalidRequestException.class);
        verify(showRepository, never()).save(any());
    }

    @Test
    void create_rejects_an_unknown_movie() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(START, END)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_rejects_an_archived_movie() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie(MovieStatus.ARCHIVED)));

        assertThatThrownBy(() -> service.create(request(START, END)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void create_rejects_an_unknown_screen() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie(MovieStatus.ACTIVE)));
        when(screenRepository.existsById(screenId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(request(START, END)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_excludes_the_show_itself_from_the_overlap_check() {
        Show existing = Show.builder().id(UUID.randomUUID()).movieId(movieId).screenId(screenId)
                .startsAt(START).endsAt(END).basePrice(BigDecimal.TEN).now(NOW).build();
        when(showRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        movieAndScreenExist();
        when(showRepository.existsOverlapping(screenId, START, END.plusSeconds(1800), existing.getId()))
                .thenReturn(false);

        ShowResponse response = service.update(existing.getId(), request(START, END.plusSeconds(1800)));

        assertThat(response.endsAt()).isEqualTo(END.plusSeconds(1800));
    }

    @Test
    void update_of_a_cancelled_show_is_a_conflict() {
        Show existing = Show.builder().id(UUID.randomUUID()).movieId(movieId).screenId(screenId)
                .startsAt(START).endsAt(END).basePrice(BigDecimal.TEN).now(NOW).build();
        existing.cancel(NOW);
        when(showRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(existing.getId(), request(START, END)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancel_is_idempotent() {
        Show existing = Show.builder().id(UUID.randomUUID()).movieId(movieId).screenId(screenId)
                .startsAt(START).endsAt(END).basePrice(BigDecimal.TEN).now(NOW).build();
        when(showRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThat(service.cancel(existing.getId()).status()).isEqualTo(ShowStatus.CANCELLED);
        assertThat(service.cancel(existing.getId()).status()).isEqualTo(ShowStatus.CANCELLED);
    }

    @Test
    void get_of_an_unknown_show_is_not_found() {
        UUID id = UUID.randomUUID();
        when(showRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(ResourceNotFoundException.class);
    }
}
