package com.movie.booking.catalog_service.service;

import com.movie.booking.catalog_service.MovieStatus;
import com.movie.booking.catalog_service.ShowStatus;
import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.dto.ShowRequest;
import com.movie.booking.catalog_service.dto.ShowResponse;
import com.movie.booking.catalog_service.entity.Movie;
import com.movie.booking.catalog_service.entity.Show;
import com.movie.booking.catalog_service.exception.ConflictException;
import com.movie.booking.catalog_service.exception.InvalidRequestException;
import com.movie.booking.catalog_service.exception.ResourceNotFoundException;
import com.movie.booking.catalog_service.mapper.CatalogMapper;
import com.movie.booking.catalog_service.repository.CatalogSpecs;
import com.movie.booking.catalog_service.repository.MovieRepository;
import com.movie.booking.catalog_service.repository.ScreenRepository;
import com.movie.booking.catalog_service.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShowService {

    // Used as "exclude this id" when creating, so the overlap query needs no separate variant.
    private static final UUID NO_SHOW = new UUID(0L, 0L);

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final CatalogMapper mapper;
    private final Clock clock;

    @Transactional
    public ShowResponse create(ShowRequest request) {
        validate(request, NO_SHOW);
        Show show = showRepository.save(mapper.toEntity(request, Instant.now(clock)));
        return mapper.toResponse(show);
    }

    @Transactional(readOnly = true)
    public ShowResponse get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ShowResponse> list(UUID movieId, UUID theaterId, Instant from, Instant to, Pageable pageable) {
        return PageResponse.of(showRepository.findAll(CatalogSpecs.shows(movieId, theaterId, from, to), pageable),
                mapper::toResponse);
    }

    @Transactional
    public ShowResponse update(UUID id, ShowRequest request) {
        Show show = find(id);
        if (show.getStatus() == ShowStatus.CANCELLED) {
            throw new ConflictException("Show " + id + " is cancelled and cannot be changed");
        }
        validate(request, id);
        mapper.apply(request, show, Instant.now(clock));
        return mapper.toResponse(show);
    }

    // Cancelling twice is a no-op that returns the current state.
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
            throw new ConflictException("Screen " + request.screenId() + " already has a show overlapping "
                    + request.startsAt() + " to " + request.endsAt());
        }
    }

    private Show find(UUID id) {
        return showRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Show", id));
    }
}
