package com.movie.booking.catalog_service.service;

import com.movie.booking.catalog_service.config.CacheNames;
import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.dto.ScreenRequest;
import com.movie.booking.catalog_service.dto.ScreenResponse;
import com.movie.booking.catalog_service.entity.Screen;
import com.movie.booking.catalog_service.exception.ConflictException;
import com.movie.booking.catalog_service.exception.ResourceNotFoundException;
import com.movie.booking.catalog_service.mapper.ScreenMapper;
import com.movie.booking.catalog_service.repository.ScreenRepository;
import com.movie.booking.catalog_service.repository.ShowRepository;
import com.movie.booking.catalog_service.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final TheaterRepository theaterRepository;
    private final ShowRepository showRepository;
    private final ScreenMapper mapper;
    private final Clock clock;

    @Transactional
    public ScreenResponse create(UUID theaterId, ScreenRequest request) {
        requireTheater(theaterId);
        if (screenRepository.existsByTheaterIdAndName(theaterId, request.name())) {
            throw nameTaken(theaterId, request.name());
        }
        Screen screen = screenRepository.save(mapper.toEntity(theaterId, request, UUID.randomUUID(), Instant.now(clock)));
        return mapper.toResponse(screen);
    }

    @Transactional(readOnly = true)
    public PageResponse<ScreenResponse> list(UUID theaterId, Pageable pageable) {
        requireTheater(theaterId);
        return PageResponse.of(screenRepository.findByTheaterId(theaterId, pageable), mapper::toResponse);
    }

    // The key includes the theater because a screen id under the wrong theater is a 404, not a hit.
    @Cacheable(cacheNames = CacheNames.SCREENS, key = "#theaterId + ':' + #screenId")
    @Transactional(readOnly = true)
    public ScreenResponse get(UUID theaterId, UUID screenId) {
        return mapper.toResponse(find(theaterId, screenId));
    }

    @Transactional(readOnly = true)
    public Map<UUID, ScreenResponse> getByIds(Collection<UUID> ids) {
        return screenRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Screen::getId, mapper::toResponse));
    }

    @CacheEvict(cacheNames = CacheNames.SCREENS, key = "#theaterId + ':' + #screenId")
    @Transactional
    public ScreenResponse update(UUID theaterId, UUID screenId, ScreenRequest request) {
        Screen screen = find(theaterId, screenId);
        if (screenRepository.existsByTheaterIdAndNameAndIdNot(theaterId, request.name(), screenId)) {
            throw nameTaken(theaterId, request.name());
        }
        mapper.apply(request, screen, Instant.now(clock));
        return mapper.toResponse(screen);
    }

    @CacheEvict(cacheNames = CacheNames.SCREENS, key = "#theaterId + ':' + #screenId")
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
