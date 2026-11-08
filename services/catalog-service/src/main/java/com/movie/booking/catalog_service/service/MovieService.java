package com.movie.booking.catalog_service.service;

import com.movie.booking.catalog_service.MovieStatus;
import com.movie.booking.catalog_service.dto.MovieRequest;
import com.movie.booking.catalog_service.dto.MovieResponse;
import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.entity.Movie;
import com.movie.booking.catalog_service.exception.ConflictException;
import com.movie.booking.catalog_service.exception.ResourceNotFoundException;
import com.movie.booking.catalog_service.mapper.MovieMapper;
import com.movie.booking.catalog_service.repository.CatalogSpecs;
import com.movie.booking.catalog_service.repository.MovieRepository;
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
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final MovieMapper mapper;
    private final Clock clock;

    @Transactional
    public MovieResponse create(MovieRequest request) {
        Movie movie = movieRepository.save(mapper.toEntity(request, UUID.randomUUID(), Instant.now(clock)));
        return mapper.toResponse(movie);
    }

    @Transactional(readOnly = true)
    public MovieResponse get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MovieResponse> list(MovieStatus status, String titleContains, Pageable pageable) {
        return PageResponse.of(movieRepository.findAll(CatalogSpecs.movies(status, titleContains), pageable),
                mapper::toResponse);
    }

    @Transactional
    public MovieResponse update(UUID id, MovieRequest request) {
        Movie movie = find(id);
        mapper.apply(request, movie, Instant.now(clock));
        return mapper.toResponse(movie);
    }

    // Soft delete: the row stays so old shows and bookings can still point at it.
    @Transactional
    public void archive(UUID id) {
        Movie movie = find(id);
        if (showRepository.existsByMovieId(id)) {
            throw new ConflictException("Movie " + id + " has shows and cannot be deleted");
        }
        if (movie.getStatus() != MovieStatus.ARCHIVED) {
            movie.archive(Instant.now(clock));
        }
    }

    private Movie find(UUID id) {
        return movieRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Movie", id));
    }
}
