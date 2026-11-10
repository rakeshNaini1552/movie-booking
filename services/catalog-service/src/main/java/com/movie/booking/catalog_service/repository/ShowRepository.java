package com.movie.booking.catalog_service.repository;

import com.movie.booking.catalog_service.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ShowRepository extends JpaRepository<Show, UUID>, JpaSpecificationExecutor<Show> {

    boolean existsByMovieId(UUID movieId);

    // Cancelled shows are included, like the REST list; ordered so GraphQL output is stable.
    List<Show> findByMovieIdInOrderByStartsAtAscIdAsc(Collection<UUID> movieIds);

    boolean existsByScreenId(UUID screenId);

    @Query("""
            select count(s) > 0 from Show s
            where s.screenId in (select sc.id from Screen sc where sc.theaterId = :theaterId)
            """)
    boolean existsByTheaterId(@Param("theaterId") UUID theaterId);

    // Two shows overlap when each starts before the other ends; back-to-back shows (end == start) do not.
    @Query("""
            select count(s) > 0 from Show s
            where s.screenId = :screenId
              and s.status <> com.movie.booking.catalog_service.ShowStatus.CANCELLED
              and s.id <> :excludeId
              and s.startsAt < :endsAt
              and s.endsAt > :startsAt
            """)
    boolean existsOverlapping(@Param("screenId") UUID screenId,
                              @Param("startsAt") Instant startsAt,
                              @Param("endsAt") Instant endsAt,
                              @Param("excludeId") UUID excludeId);
}
