package com.movie.booking.catalog_service.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

// Shared settings: an unmapped target field fails the build instead of silently staying null.
@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CatalogMapperConfig {
}
