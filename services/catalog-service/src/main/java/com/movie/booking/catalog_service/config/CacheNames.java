package com.movie.booking.catalog_service.config;

// Cache names; CacheConfig prefixes them with catalog.cache.prefix in Redis (default key "catalog:movies::<id>").
public final class CacheNames {

    public static final String MOVIES = "movies";
    public static final String THEATERS = "theaters";
    public static final String SCREENS = "screens";
    public static final String SHOWS = "shows";

    private CacheNames() {}
}
