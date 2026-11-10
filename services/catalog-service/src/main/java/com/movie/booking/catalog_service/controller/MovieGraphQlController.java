package com.movie.booking.catalog_service.controller;

import com.movie.booking.catalog_service.MovieStatus;
import com.movie.booking.catalog_service.dto.MovieResponse;
import com.movie.booking.catalog_service.exception.InvalidRequestException;
import com.movie.booking.catalog_service.service.MovieService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class MovieGraphQlController {

    private final MovieService movieService;
    private final int maxPageSize;

    public MovieGraphQlController(MovieService movieService,
                                  @Value("${catalog.graphql.max-page-size}") int maxPageSize) {
        this.movieService = movieService;
        this.maxPageSize = maxPageSize;
    }

    @QueryMapping
    public MovieResponse movie(@Argument UUID id) {
        return movieService.get(id);
    }

    @QueryMapping
    public List<MovieResponse> movies(@Argument String status, @Argument String titleContains,
                                      @Argument int page, @Argument int size) {
        if (page < 0 || size < 1) {
            throw new InvalidRequestException("page must be >= 0 and size must be >= 1");
        }
        // Sorted so that paging is stable; the REST list has no default order.
        PageRequest pageable = PageRequest.of(page, Math.min(size, maxPageSize), Sort.by("title", "id"));
        return movieService.search(parseStatus(status), titleContains, pageable);
    }

    private static MovieStatus parseStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return MovieStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unknown movie status '" + status + "'");
        }
    }
}
