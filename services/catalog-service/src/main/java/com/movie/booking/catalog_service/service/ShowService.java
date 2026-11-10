package com.movie.booking.catalog_service.service;

import com.movie.booking.catalog_service.MovieStatus;
import com.movie.booking.catalog_service.ShowStatus;
import com.movie.booking.catalog_service.config.CacheNames;
import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.dto.ShowRequest;
import com.movie.booking.catalog_service.dto.ShowResponse;
import com.movie.booking.catalog_service.entity.Movie;
import com.movie.booking.catalog_service.entity.Show;
import com.movie.booking.catalog_service.exception.ConflictException;
import com.movie.booking.catalog_service.exception.InvalidRequestException;
import com.movie.booking.catalog_service.exception.ResourceNotFoundException;
import com.movie.booking.catalog_service.mapper.ShowMapper;
import com.movie.booking.catalog_service.repository.CatalogSpecs;
import com.movie.booking.catalog_service.repository.MovieRepository;
import com.movie.booking.catalog_service.repository.ScreenRepository;
import com.movie.booking.catalog_service.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowService {

    // Used as "exclude this id" when creating, so the overlap query needs no separate variant.
    private static final UUID NO_SHOW = new UUID(0L, 0L);

    // Exclusion constraint (V3) and the older same-start unique index (V2): both mean the slot is taken.
    private static final List<String> OVERLAP_CONSTRAINTS = List.of("show_screen_no_overlap", "show_screen_starts_active_unique");

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final ShowMapper mapper;
    private final Clock clock;

    @Transactional
    public ShowResponse create(ShowRequest request) {
        validate(request, NO_SHOW);
        Show show = flushOrConflict(request, () -> showRepository.saveAndFlush(
                mapper.toEntity(request, UUID.randomUUID(), Instant.now(clock))));
        return mapper.toResponse(show);
    }

    @Cacheable(cacheNames = CacheNames.SHOWS, key = "#id")
    @Transactional(readOnly = true)
    public ShowResponse get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ShowResponse> list(UUID movieId, UUID theaterId, Instant from, Instant to, Pageable pageable) {
        return PageResponse.of(showRepository.findAll(CatalogSpecs.shows(movieId, theaterId, from, to), pageable),
                mapper::toResponse);
    }

    // One query for all the movies of a GraphQL batch; every requested id gets an entry, empty if it has no shows.
    @Transactional(readOnly = true)
    public Map<UUID, List<ShowResponse>> listByMovieIds(Collection<UUID> movieIds) {
        Map<UUID, List<ShowResponse>> byMovie = showRepository.findByMovieIdInOrderByStartsAtAscIdAsc(movieIds).stream()
                .collect(Collectors.groupingBy(Show::getMovieId, Collectors.mapping(mapper::toResponse, Collectors.toList())));
        movieIds.forEach(id -> byMovie.putIfAbsent(id, List.of()));
        return byMovie;
    }

    @CacheEvict(cacheNames = CacheNames.SHOWS, key = "#id")
    @Transactional
    public ShowResponse update(UUID id, ShowRequest request) {
        Show show = find(id);
        if (show.getStatus() == ShowStatus.CANCELLED) {
            throw new ConflictException("Show " + id + " is cancelled and cannot be changed");
        }
        validate(request, id);
        mapper.apply(request, show, Instant.now(clock));
        flushOrConflict(request, () -> {
            showRepository.flush();
            return show;
        });
        return mapper.toResponse(show);
    }

    // Cancelling twice is a no-op that returns the current state.
    @CacheEvict(cacheNames = CacheNames.SHOWS, key = "#id")
    @Transactional
    public ShowResponse cancel(UUID id) {
        Show show = find(id);
        if (show.getStatus() != ShowStatus.CANCELLED) {
            show.cancel(Instant.now(clock));
        }
        return mapper.toResponse(show);
    }

    private void validate(ShowRequest request, UUID excludeShowId) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new InvalidRequestException("endsAt must be after startsAt");
        }
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie", request.movieId()));
        if (movie.getStatus() == MovieStatus.ARCHIVED) {
            throw new ConflictException("Movie " + movie.getId() + " is archived and cannot be scheduled");
        }
        if (!screenRepository.existsById(request.screenId())) {
            throw new ResourceNotFoundException("Screen", request.screenId());
        }
        if (showRepository.existsOverlapping(request.screenId(), request.startsAt(), request.endsAt(), excludeShowId)) {
            throw overlap(request);
        }
    }

    // The check above can pass for two concurrent requests; the database constraints then reject the second write.
    // Flushing here turns that violation into the same 409 the check gives, instead of failing later at commit.
    private <T> T flushOrConflict(ShowRequest request, Supplier<T> write) {
        try {
            return write.get();
        } catch (DataIntegrityViolationException e) {
            String cause = String.valueOf(e.getMostSpecificCause().getMessage());
            if (OVERLAP_CONSTRAINTS.stream().anyMatch(cause::contains)) {
                throw overlap(request);
            }
            throw e;
        } catch (PessimisticLockingFailureException e) {
            // Two simultaneous inserts into the exclusion constraint can make Postgres abort one of them as a
            // deadlock victim instead of reporting the violation. Either way the slot was taken by the other write.
            throw overlap(request);
        }
    }

    private ConflictException overlap(ShowRequest request) {
        return new ConflictException("Screen " + request.screenId() + " already has a show overlapping "
                + request.startsAt() + " to " + request.endsAt());
    }

    private Show find(UUID id) {
        return showRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Show", id));
    }
}
