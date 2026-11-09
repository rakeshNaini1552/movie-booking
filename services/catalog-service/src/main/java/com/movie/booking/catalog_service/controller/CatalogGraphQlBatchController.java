package com.movie.booking.catalog_service.controller;

import com.movie.booking.catalog_service.dto.MovieResponse;
import com.movie.booking.catalog_service.dto.ScreenResponse;
import com.movie.booking.catalog_service.dto.ShowResponse;
import com.movie.booking.catalog_service.dto.TheaterResponse;
import com.movie.booking.catalog_service.service.ScreenService;
import com.movie.booking.catalog_service.service.ShowService;
import com.movie.booking.catalog_service.service.TheaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// Nested fields are resolved per batch of parents (one query per level) instead of once per parent.
@Controller
@RequiredArgsConstructor
public class CatalogGraphQlBatchController {

    private final ShowService showService;
    private final ScreenService screenService;
    private final TheaterService theaterService;

    @BatchMapping(typeName = "Movie", field = "shows")
    public Map<MovieResponse, List<ShowResponse>> shows(List<MovieResponse> movies) {
        Map<UUID, List<ShowResponse>> byMovie = showService.listByMovieIds(ids(movies, MovieResponse::id));
        return movies.stream().collect(Collectors.toMap(Function.identity(), m -> byMovie.get(m.id())));
    }

    @BatchMapping(typeName = "Show", field = "screen")
    public Map<ShowResponse, ScreenResponse> screen(List<ShowResponse> shows) {
        Map<UUID, ScreenResponse> byId = screenService.getByIds(ids(shows, ShowResponse::screenId));
        return shows.stream().collect(Collectors.toMap(Function.identity(), s -> byId.get(s.screenId())));
    }

    @BatchMapping(typeName = "Screen", field = "theater")
    public Map<ScreenResponse, TheaterResponse> theater(List<ScreenResponse> screens) {
        Map<UUID, TheaterResponse> byId = theaterService.getByIds(ids(screens, ScreenResponse::theaterId));
        return screens.stream().collect(Collectors.toMap(Function.identity(), s -> byId.get(s.theaterId())));
    }

    private static <T> List<UUID> ids(List<T> parents, Function<T, UUID> id) {
        return parents.stream().map(id).distinct().toList();
    }
}
