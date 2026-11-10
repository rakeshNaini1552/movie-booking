package com.movie.booking.catalog_service.service;

import com.movie.booking.catalog_service.config.CacheNames;
import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.dto.TheaterRequest;
import com.movie.booking.catalog_service.dto.TheaterResponse;
import com.movie.booking.catalog_service.entity.Theater;
import com.movie.booking.catalog_service.exception.ConflictException;
import com.movie.booking.catalog_service.exception.ResourceNotFoundException;
import com.movie.booking.catalog_service.mapper.TheaterMapper;
import com.movie.booking.catalog_service.repository.CatalogSpecs;
import com.movie.booking.catalog_service.repository.ScreenRepository;
import com.movie.booking.catalog_service.repository.ShowRepository;
import com.movie.booking.catalog_service.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
public class TheaterService {

    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final ShowRepository showRepository;
    private final TheaterMapper mapper;
    private final Clock clock;

    @Transactional
    public TheaterResponse create(TheaterRequest request) {
        Theater theater = theaterRepository.save(mapper.toEntity(request, UUID.randomUUID(), Instant.now(clock)));
        return mapper.toResponse(theater);
    }

    @Cacheable(cacheNames = CacheNames.THEATERS, key = "#id")
    @Transactional(readOnly = true)
    public TheaterResponse get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TheaterResponse> list(String city, Pageable pageable) {
        return PageResponse.of(theaterRepository.findAll(CatalogSpecs.theaters(city), pageable),
                mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<UUID, TheaterResponse> getByIds(Collection<UUID> ids) {
        return theaterRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Theater::getId, mapper::toResponse));
    }

    @CacheEvict(cacheNames = CacheNames.THEATERS, key = "#id")
    @Transactional
    public TheaterResponse update(UUID id, TheaterRequest request) {
        Theater theater = find(id);
        mapper.apply(request, theater, Instant.now(clock));
        return mapper.toResponse(theater);
    }

    // Hard delete: its screens go with it, but only when none of them has ever had a show.
    // The theater's screens are deleted with it, so every cached screen entry is dropped too (rare operation).
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.THEATERS, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.SCREENS, allEntries = true)})
    @Transactional
    public void delete(UUID id) {
        Theater theater = find(id);
        if (showRepository.existsByTheaterId(id)) {
            throw new ConflictException("Theater " + id + " has shows and cannot be deleted");
        }
        screenRepository.deleteByTheaterId(id);
        theaterRepository.delete(theater);
    }

    private Theater find(UUID id) {
        return theaterRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Theater", id));
    }
}
