package com.movie.booking.catalog_service.service;

import com.movie.booking.catalog_service.dto.ScreenRequest;
import com.movie.booking.catalog_service.entity.Screen;
import com.movie.booking.catalog_service.entity.Theater;
import com.movie.booking.catalog_service.exception.ConflictException;
import com.movie.booking.catalog_service.exception.ResourceNotFoundException;
import com.movie.booking.catalog_service.mapper.CatalogMapper;
import com.movie.booking.catalog_service.repository.ScreenRepository;
import com.movie.booking.catalog_service.repository.ShowRepository;
import com.movie.booking.catalog_service.repository.TheaterRepository;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TheaterAndScreenServiceTest {

    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @Mock private TheaterRepository theaterRepository;
    @Mock private ScreenRepository screenRepository;
    @Mock private ShowRepository showRepository;

    private TheaterService theaterService;
    private ScreenService screenService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        theaterService = new TheaterService(theaterRepository, screenRepository, showRepository,
                new CatalogMapper(), clock);
        screenService = new ScreenService(screenRepository, theaterRepository, showRepository,
                new CatalogMapper(), clock);
    }

    @Test
    void deleting_a_theater_with_shows_is_a_conflict_and_deletes_nothing() {
        Theater theater = Theater.builder().id(UUID.randomUUID()).name("PVR").city("Pune").address("MG Road")
                .now(NOW).build();
        when(theaterRepository.findById(theater.getId())).thenReturn(Optional.of(theater));
        when(showRepository.existsByTheaterId(theater.getId())).thenReturn(true);

        assertThatThrownBy(() -> theaterService.delete(theater.getId())).isInstanceOf(ConflictException.class);
        verify(screenRepository, never()).deleteByTheaterId(any());
        verify(theaterRepository, never()).delete(any(Theater.class));
    }

    @Test
    void deleting_a_theater_without_shows_removes_its_screens_and_the_theater() {
        Theater theater = Theater.builder().id(UUID.randomUUID()).name("PVR").city("Pune").address("MG Road")
                .now(NOW).build();
        when(theaterRepository.findById(theater.getId())).thenReturn(Optional.of(theater));
        when(showRepository.existsByTheaterId(theater.getId())).thenReturn(false);

        theaterService.delete(theater.getId());

        verify(screenRepository).deleteByTheaterId(theater.getId());
        verify(theaterRepository).delete(theater);
    }

    @Test
    void creating_a_screen_under_an_unknown_theater_is_not_found() {
        UUID theaterId = UUID.randomUUID();
        when(theaterRepository.existsById(theaterId)).thenReturn(false);

        assertThatThrownBy(() -> screenService.create(theaterId, new ScreenRequest("Screen 1", 10, 12)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void creating_a_screen_with_a_taken_name_is_a_conflict() {
        UUID theaterId = UUID.randomUUID();
        when(theaterRepository.existsById(theaterId)).thenReturn(true);
        when(screenRepository.existsByTheaterIdAndName(theaterId, "Screen 1")).thenReturn(true);

        assertThatThrownBy(() -> screenService.create(theaterId, new ScreenRequest("Screen 1", 10, 12)))
                .isInstanceOf(ConflictException.class);
        verify(screenRepository, never()).save(any());
    }

    @Test
    void deleting_a_screen_with_shows_is_a_conflict() {
        Screen screen = Screen.builder().id(UUID.randomUUID()).theaterId(UUID.randomUUID()).name("Screen 1")
                .seatRows(10).seatsPerRow(12).now(NOW).build();
        when(screenRepository.findByIdAndTheaterId(screen.getId(), screen.getTheaterId()))
                .thenReturn(Optional.of(screen));
        when(showRepository.existsByScreenId(screen.getId())).thenReturn(true);

        assertThatThrownBy(() -> screenService.delete(screen.getTheaterId(), screen.getId()))
                .isInstanceOf(ConflictException.class);
        verify(screenRepository, never()).delete(any(Screen.class));
    }

    @Test
    void a_screen_under_the_wrong_theater_is_not_found() {
        UUID theaterId = UUID.randomUUID();
        UUID screenId = UUID.randomUUID();
        when(screenRepository.findByIdAndTheaterId(screenId, theaterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> screenService.get(theaterId, screenId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
