package com.movie.booking.catalog_service.config;

import com.movie.booking.catalog_service.dto.MovieResponse;
import com.movie.booking.catalog_service.dto.ScreenResponse;
import com.movie.booking.catalog_service.dto.ShowResponse;
import com.movie.booking.catalog_service.dto.TheaterResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

// Caches single-item reads by id in Redis. Switched off completely with catalog.cache.enabled=false.
// Keys are "<catalog.cache.prefix><cacheName>::<id>"; the prefix keeps environments that share one Redis apart.
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "catalog.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig implements CachingConfigurer {

    @Bean
    CacheManager cacheManager(RedisConnectionFactory connectionFactory, JsonMapper jsonMapper,
                              @Value("${catalog.cache.ttl}") Duration ttl,
                              @Value("${catalog.cache.prefix}") String prefix) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .prefixCacheNameWith(prefix)
                .serializeKeysWith(SerializationPair.fromSerializer(StringRedisSerializer.UTF_8));
        // One typed JSON serializer per cache, so no class names are stored in Redis and values stay readable.
        Map<String, RedisCacheConfiguration> caches = Map.of(
                CacheNames.MOVIES, valuesAs(base, jsonMapper, MovieResponse.class),
                CacheNames.THEATERS, valuesAs(base, jsonMapper, TheaterResponse.class),
                CacheNames.SCREENS, valuesAs(base, jsonMapper, ScreenResponse.class),
                CacheNames.SHOWS, valuesAs(base, jsonMapper, ShowResponse.class));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(caches)
                .enableStatistics()          // exposes cache.gets{result=hit|miss} through Micrometer
                .transactionAware()          // puts and evictions happen after the surrounding commit
                .build();
    }

    private static <T> RedisCacheConfiguration valuesAs(RedisCacheConfiguration base, JsonMapper jsonMapper,
                                                         Class<T> type) {
        return base.serializeValuesWith(SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(jsonMapper, type)));
    }

    // A cache problem (Redis down, bad payload) is a miss, never a failed request.
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache get failed, reading from the database: cache={} key={} cause={}", cache.getName(), key, e.toString());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache put failed: cache={} key={} cause={}", cache.getName(), key, e.toString());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache evict failed, the entry expires after the TTL: cache={} key={} cause={}", cache.getName(), key, e.toString());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache clear failed, entries expire after the TTL: cache={} cause={}", cache.getName(), e.toString());
            }
        };
    }
}
