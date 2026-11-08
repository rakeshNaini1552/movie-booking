package com.movie.booking.catalog_service.service;

import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.dto.ScreenRequest;
import com.movie.booking.catalog_service.dto.ScreenResponse;
import com.movie.booking.catalog_service.entity.Screen;
import com.movie.booking.catalog_service.exception.ConflictException;
import com.movie.booking.catalog_service.exception.ResourceNotFoundException;
import com.movie.booking.catalog_service.mapper.CatalogMapper;
import com.movie.booking.catalog_service.repository.ScreenRepository;
import com.movie.booking.catalog_service.repository.ShowRepository;
import com.movie.booking.catalog_service.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final TheaterRepository theaterRepository;
    private final ShowRepository showRepository;
    private final CatalogMapper mapper;
    private final Clock clock;

    @Transactional
    public ScreenResponse create(UUID theaterId, ScreenRequest request) {
        requireTheater(theaterId);
        if (screenRepository.existsByTheaterIdAndName(theaterId, request.name())) {
            throw nameTaken(theaterId, request.name());
        }
        Screen screen = screenRepository.save(mapper.toEntity(theaterId, request, Instant.now(clock)));
        return mapper.toResponse(screen);
    }

    @Transactional(readOnly = true)
    public PageResponse<ScreenResponse> list(UUID theaterId, Pageable pageable) {
        requireTheater(theaterId);
        return PageResponse.of(screenRepository.findByTheaterId(theaterId, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ScreenResponse get(UUID theaterId, UUID screenId) {
        return mapper.toResponse(find(theaterId, screenId));
    }

    @Transactional
    public ScreenResponse update(UUID theaterId, UUID screenId, ScreenRequest request) {
        Screen screen = find(theaterId, screenId);
        if (screenRepository.existsByTheaterIdAndNameAndIdNot(theaterId, request.name(), screenId)) {
            throw nameTaken(theaterId, request.name());
        }
        mapper.apply(request, screen, Instant.now(clock));
        return mapper.toResponse(screen);
    }

    @Transactional
    public void delete(UUID theaterId, UUID screenId) {
        Screen screen = find(theaterId, screenId);
        if (showRepository.existsByScreenId(screenId)) {
            throw new ConflictException("Screen " + screenId + " has shows and cannot be deleted");
        }
        screenRepository.delete(screen);
    }

    private void requireTheater(UUID theaterId) {
        if (!theaterRepository.existsById(theaterId)) {
            throw new ResourceNotFoundException("Theater", theaterId);
        }
    }

    // Looked up through the theater so a screen id under the wrong theater is a 404, not a leak.
    private Screen find(UUID theaterId, UUID screenId) {
        return screenRepository.findByIdAndTheaterId(screenId, theaterId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", screenId));
    }

    private ConflictException nameTaken(UUID theaterId, String name) {
        return new ConflictException("Theater " + theaterId + " already has a screen named '" + name + "'");
    }
}
