package com.movie.booking.catalog_service.repository;

import com.movie.booking.catalog_service.MovieStatus;
import com.movie.booking.catalog_service.entity.Movie;
import com.movie.booking.catalog_service.entity.Screen;
import com.movie.booking.catalog_service.entity.Show;
import com.movie.booking.catalog_service.entity.Theater;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// Optional list filters. A null argument means "do not filter on this", so the queries never bind null parameters.
public final class CatalogSpecs {

    private CatalogSpecs() {}

    public static Specification<Movie> movies(MovieStatus status, String titleContains) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (titleContains != null && !titleContains.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + escapeLike(titleContains.trim().toLowerCase(Locale.ROOT)) + "%", '\\'));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Theater> theaters(String city) {
        return (root, query, cb) -> city == null || city.isBlank()
                ? cb.conjunction()
                : cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase(Locale.ROOT));
    }

    public static Specification<Show> shows(UUID movieId, UUID theaterId, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (movieId != null) {
                predicates.add(cb.equal(root.get("movieId"), movieId));
            }
            if (theaterId != null) {
                Subquery<UUID> screens = query.subquery(UUID.class);
                Root<Screen> screen = screens.from(Screen.class);
                screens.select(screen.get("id")).where(cb.equal(screen.get("theaterId"), theaterId));
                predicates.add(root.get("screenId").in(screens));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startsAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("startsAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
